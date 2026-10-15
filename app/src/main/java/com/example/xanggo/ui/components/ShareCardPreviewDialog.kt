package com.example.xanggo.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.xanggo.i18n.tr
import com.example.xanggo.theme.*
import com.example.xanggo.util.ShareCardGenerator
import com.example.xanggo.util.ShareCardPreviewData

@Composable
fun ShareCardPreviewDialog(
    previewData: ShareCardPreviewData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val imageBitmap = remember(previewData.bitmap) { previewData.bitmap.asImageBitmap() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(22.dp)),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, PetrolTeal.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📸", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = tr("Xem trước ảnh chia sẻ", "Share Image Preview"),
                                color = TextPrimaryDark,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr("Ảnh HD sắc nét gửi Zalo, Messenger, Facebook", "HD card ready for Zalo, Messenger, Facebook"),
                                color = TextSecondaryDark,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Surface(
                        color = DarkBackground,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable { onDismiss() }
                    ) {
                        Text(
                            text = "✕",
                            color = TextSecondaryDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Khung hiển thị tấm ảnh vừa được vẽ
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0B1120))
                        .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = tr("Ảnh bảng giá xăng XăngGo", "XăngGo Fuel Price Card"),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Nút Gửi ngay nổi bật
                Button(
                    onClick = {
                        ShareCardGenerator.shareRenderedCard(context, previewData)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PetrolTeal,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = tr("📤 Gửi qua Zalo / Facebook / Messenger", "📤 Share to Zalo / Facebook / Apps"),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tr("Đóng lại", "Close"),
                        color = TextSecondaryDark,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
