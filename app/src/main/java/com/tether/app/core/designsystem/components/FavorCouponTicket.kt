package com.tether.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tether.app.R
import com.tether.app.core.designsystem.theme.PixelCouponBorder
import com.tether.app.core.designsystem.theme.PixelDarkInk
import com.tether.app.core.designsystem.theme.PixelFont
import com.tether.app.core.designsystem.theme.PixelFrameCard
import com.tether.app.core.designsystem.theme.PixelMutedInk
import com.tether.app.core.designsystem.theme.PixelStampBg
import com.tether.app.core.designsystem.theme.PixelStampRed

@Composable
fun FavorCouponTicket(
    title: String,
    description: String,
    isClaimed: Boolean,
    onClaimClick: () -> Unit,
    modifier: Modifier = Modifier,
    rotation: Float = 0f
) {
    Box(
        modifier = modifier
            .width(250.dp)
            .graphicsLayer { rotationZ = rotation }
            .pixelShadow(3.dp, 3.dp)
            .fillMaxWidth()
            .background(PixelFrameCard)
            .border(2.dp, PixelDarkInk)
            .clickable(enabled = !isClaimed) { onClaimClick() }
            .padding(10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_pixel_coupon),
                        contentDescription = "Kupon",
                        modifier = Modifier.size(14.dp),
                        tint = PixelMutedInk
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title.uppercase(),
                        fontFamily = PixelFont,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = PixelMutedInk,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = description,
                    fontFamily = PixelFont,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PixelDarkInk,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (!isClaimed) {
                    Text(
                        text = "[ KETUK UNTUK KLAIM ]",
                        fontFamily = PixelFont,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = PixelCouponBorder
                    )
                }
            }

            // Stempel Pixel Merah "CLAIMED"
            if (isClaimed) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(PixelStampBg)
                        .border(1.5.dp, PixelStampRed)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "CLAIMED",
                        fontFamily = PixelFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = PixelStampRed
                    )
                }
            }
        }
}
