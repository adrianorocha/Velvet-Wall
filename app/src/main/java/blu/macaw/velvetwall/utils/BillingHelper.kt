package blu.macaw.velvetwall.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import blu.macaw.velvetwall.data.UserSettings
import com.android.billingclient.BuildConfig

class BillingHelper(
    private val context: Context,
    private val userSettings: UserSettings,
    private val onSuccess: () -> Unit // <--- O "telefone" para avisar o ViewModel
) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val isDebug = true

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                // Aqui chamamos aquela função de confirmação que criamos!
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.i("VELVET_BILLING", "Usuário desistiu da compra.")
        } else {
            Log.e("VELVET_BILLING", "Erro no faturamento: ${billingResult.debugMessage}")
        }
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    checkCurrentPurchases()
                }
            }
            override fun onBillingServiceDisconnected() {
                startConnection()
            }
        })
    }
    fun launchPurchaseFlow(activity: Activity) {
        // 1. Verificação de Conexão
        if (!billingClient.isReady) {
            Log.e("VELVET_BILLING", "❌ BillingClient não está pronto!")
            Toast.makeText(context, "Serviço da Play Store não está pronto. Tentando reconectar...", Toast.LENGTH_LONG).show()
            startConnection()
            return
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("velvet_wall_pro_lifetime")
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        Log.d("VELVET_BILLING", "🔍 Consultando produto: velvet_wall_pro_lifetime...")

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            // 2. Log do Resultado da Consulta
            VelvetBillingLogger.logResult("QUERY_PRODUCT", billingResult)

            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val productDetails = productDetailsList.firstOrNull()

                if (productDetails == null) {
                    // ERRO MAIS COMUM: O Google não encontrou o ID
                    Log.e("VELVET_BILLING", "❌ Produto não encontrado no Console! Verifique o ID.")
                    activity.runOnUiThread {
                        Toast.makeText(context, "Erro: Produto não encontrado na Play Store.", Toast.LENGTH_LONG).show()
                    }
                    return@queryProductDetailsAsync
                }

                val productDetailsParamsList = listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )

                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()

                Log.d("VELVET_BILLING", "🚀 Lançando fluxo de compra...")
                billingClient.launchBillingFlow(activity, billingFlowParams)
            } else {
                activity.runOnUiThread {
                    Toast.makeText(context, "Erro Play Store: ${billingResult.debugMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    private fun handlePurchase(purchase: Purchase) {
        // 1. Verifica se o status é realmente "Comprado" (e não "Pendente")
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {

            // 2. Verifica se a compra AINDA NÃO foi confirmada
            if (!purchase.isAcknowledged) {

                // 3. Monta o pacote de confirmação com o Token único da compra
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                // 4. Envia para o Google
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d("VELVET_BILLING", "✅ Compra confirmada com sucesso no Google!")

                        // AQUI VOCÊ ATUALIZA O ESTADO:
                        onSuccess()
                        // Exemplo: onPurchaseSuccess(true) ou chamando o ViewModel
                    } else {
                        Log.e("VELVET_BILLING", "❌ Erro ao confirmar compra: ${billingResult.debugMessage}")
                    }
                }
            } else {
                // Se já estava confirmada (ex: o usuário reinstalou o app)
                Log.d("VELVET_BILLING", "✅ Compra já estava confirmada anteriormente.")
                // Apenas atualize o estado local para liberar o modo PRO
            }
        }
    }


    fun checkExistingPurchases(onStateUpdated: (Boolean) -> Unit) {
        if (!billingClient.isReady) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchaseList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                // Verifica se o ID "velvet_wall_pro_lifetime" está na lista e se foi comprado
                val isPro = purchaseList.any {
                    it.products.contains("velvet_wall_pro_lifetime") &&
                            it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                onStateUpdated(isPro)
            }
        }
    }

    fun queryExistingPurchases(onResult: (Boolean) -> Unit) {
        if (!billingClient.isReady) {
            Log.e("VELVET", "BillingClient não está pronto. Reiniciando conexão.")
            onResult(false) // <--- OBRIGATÓRIO: Avisa o ViewModel para parar a rodinha
            startConnection()
            return
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { result, list ->
            // Verifica o ID com hifens
            val hasPro = result.responseCode == BillingClient.BillingResponseCode.OK &&
                    list.any { it.products.contains("velvet_wall_pro_lifetime") }

            // Se a lista estiver vazia ou o código for diferente de OK, hasPro será false
            onResult(hasPro)
        }
    }
    fun checkCurrentPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val isPro = purchases.any {
                    it.products.contains("velvet_wall_pro_lifetime") &&
                            it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                scope.launch { userSettings.setPremium(isPro) }
            }
        }
    }

    fun restorePurchases() = checkCurrentPurchases()
}