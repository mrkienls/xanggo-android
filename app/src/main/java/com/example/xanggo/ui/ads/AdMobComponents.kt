package com.example.xanggo.ui.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

object AdMobConfig {
    // ID Test chính thức của Google (an toàn 100% trong quá trình phát triển & kiểm thử)
    const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"

    // Khi bạn tạo xong Ad Units trên Google AdMob, chỉ cần thay ID thật vào 2 biến này:
    var BANNER_AD_UNIT_ID: String = TEST_BANNER_ID
    var NATIVE_AD_UNIT_ID: String = TEST_NATIVE_ID
}

@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobConfig.BANNER_AD_UNIT_ID
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
