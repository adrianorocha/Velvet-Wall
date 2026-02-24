package blu.macaw.velvetwall.utils // Certifique-se de que o package está igual ao do BillingHelper

import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase

object VelvetBillingLogger {
    private const val TAG = "VELVET_BILLING"

    /**
     * Traduz códigos do Google Play Billing para mensagens legíveis.
     */
    fun logResult(operation: String, result: BillingResult) {
        val message = when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> "✅ SUCESSO"
            BillingClient.BillingResponseCode.USER_CANCELED -> "❌ CANCELADO"
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> "💎 JÁ É PRO"
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> "🛠️ ERRO TÉCNICO (ID INVÁLIDO?)"
            else -> "❓ CÓDIGO: ${result.responseCode} - ${result.debugMessage}"
        }
        Log.d(TAG, "[$operation]: $message")
    }

    /**
     * Monitora o estado da compra em tempo real.
     */
    fun logPurchaseState(purchase: Purchase) {
        val state = when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> "💰 COMPRADO"
            Purchase.PurchaseState.PENDING -> "⏳ PENDENTE"
            else -> "⚠️ OUTRO"
        }
        Log.i(TAG, "STATUS: $state | ID: ${purchase.orderId}")
    }
}