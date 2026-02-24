package blu.macaw.velvetwall.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import blu.macaw.velvetwall.data.UserSettings

class BillingHelper(
    private val context: Context,
    private val userSettings: UserSettings,
    private val onSuccess: () -> Unit // <--- O "telefone" para avisar o ViewModel
) {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val billingClient = BillingClient.newBuilder(context)
        .setListener { billingResult, purchases ->
            VelvetBillingLogger.logResult("LISTENER_COMPRA", billingResult)
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            }
        }
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
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("velvet_wall_pro_lifetime")
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val productDetails = productDetailsList.firstOrNull() ?: return@queryProductDetailsAsync
                val productDetailsParamsList = listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()

                billingClient.launchBillingFlow(activity, billingFlowParams)
            }
        }
    }

    fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            scope.launch {
                userSettings.setPremium(true)
            }

            if (!purchase.isAcknowledged) {
                val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(acknowledgeParams) { result ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d("VELVET_BILLING", "✅ Compra confirmada!")
                        onSuccess() // <--- AQUI o ViewModel é avisado para brilhar!
                    }
                }
            }
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