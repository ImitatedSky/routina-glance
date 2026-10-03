package com.routina.glance.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 從家族種子色 #82907F 算出來的完整色階，不是手挑的幾個值。
 *
 * 產生方式：在 Oklch 裡固定色相與彩度、只變明度，超出 sRGB 就把彩度收回來。
 * 選 Oklch 不選 CIELAB 是因為 Lab 提亮時色相會跑掉，Oklch 在明度變化時守得住色相。
 * 色調編號沿用 M3 的定義（= CIE L*），所以下面的 t40 / t90 可以直接對上 M3 的角色表。
 *
 * **整套都要填滿**：Material 的 ColorScheme 只要有角色沒給值，就會沿用內建的紫色基線，
 * 於是分頁列、晶片、進度條軌道會冒出與家族色無關的紫（Fit v0.2.0 踩過）。
 */

/** 主色：圖示的灰綠 */
internal object P {
    val t0 = Color(0xFF000000)
    val t4 = Color(0xFF041302)
    val t6 = Color(0xFF071805)
    val t10 = Color(0xFF0F210C)
    val t12 = Color(0xFF132510)
    val t17 = Color(0xFF1E2F1A)
    val t20 = Color(0xFF243620)
    val t22 = Color(0xFF283B24)
    val t24 = Color(0xFF2C3F29)
    val t30 = Color(0xFF3A4D36)
    val t40 = Color(0xFF51654D)
    val t50 = Color(0xFF697E65)
    val t60 = Color(0xFF83987E)
    val t70 = Color(0xFF9DB398)
    val t80 = Color(0xFFB8CEB3)
    val t87 = Color(0xFFCBE2C6)
    val t90 = Color(0xFFD3EACE)
    val t92 = Color(0xFFD9F0D4)
    val t94 = Color(0xFFDFF6DA)
    val t95 = Color(0xFFE1F9DC)
    val t96 = Color(0xFFE4FCDF)
    val t97 = Color(0xFFE7FFE2)
    val t98 = Color(0xFFEFFFEB)
    val t99 = Color(0xFFF7FFF5)
    val t100 = Color(0xFFFFFFFF)
}

/** 次要色：同色相、低彩度，用在不搶戲的容器 */
internal object S {
    val t0 = Color(0xFF000000)
    val t4 = Color(0xFF0B100A)
    val t6 = Color(0xFF10150F)
    val t10 = Color(0xFF181D17)
    val t12 = Color(0xFF1C211B)
    val t17 = Color(0xFF262C25)
    val t20 = Color(0xFF2C322B)
    val t22 = Color(0xFF31372F)
    val t24 = Color(0xFF353B34)
    val t30 = Color(0xFF434941)
    val t40 = Color(0xFF5A6159)
    val t50 = Color(0xFF737971)
    val t60 = Color(0xFF8C938A)
    val t70 = Color(0xFFA6AEA5)
    val t80 = Color(0xFFC2C9C0)
    val t87 = Color(0xFFD5DDD3)
    val t90 = Color(0xFFDDE5DC)
    val t92 = Color(0xFFE3EBE1)
    val t94 = Color(0xFFE9F1E7)
    val t95 = Color(0xFFECF3EA)
    val t96 = Color(0xFFEEF6ED)
    val t97 = Color(0xFFF1F9F0)
    val t98 = Color(0xFFF4FCF3)
    val t99 = Color(0xFFF7FFF5)
    val t100 = Color(0xFFFFFFFF)
}

/** 第三色：圖示的琥珀，只有少數強調處用得到 */
internal object T {
    val t0 = Color(0xFF000000)
    val t4 = Color(0xFF170B00)
    val t6 = Color(0xFF1E1000)
    val t10 = Color(0xFF281800)
    val t12 = Color(0xFF2E1B00)
    val t17 = Color(0xFF3B2500)
    val t20 = Color(0xFF442B00)
    val t22 = Color(0xFF492F00)
    val t24 = Color(0xFF4F3300)
    val t30 = Color(0xFF613F00)
    val t40 = Color(0xFF7C5619)
    val t50 = Color(0xFF966F35)
    val t60 = Color(0xFFB1894F)
    val t70 = Color(0xFFCDA369)
    val t80 = Color(0xFFE9BE84)
    val t87 = Color(0xFFFED297)
    val t90 = Color(0xFFFFDDAE)
    val t92 = Color(0xFFFFE4BF)
    val t94 = Color(0xFFFFEBD0)
    val t95 = Color(0xFFFFEED8)
    val t96 = Color(0xFFFFF1E0)
    val t97 = Color(0xFFFFF5E8)
    val t98 = Color(0xFFFFF8EF)
    val t99 = Color(0xFFFFFCF7)
    val t100 = Color(0xFFFFFFFF)
}

/** 中性色：圖示奶油底的暖灰，不是純灰 */
internal object N {
    val t0 = Color(0xFF000000)
    val t4 = Color(0xFF100E0A)
    val t6 = Color(0xFF15130F)
    val t10 = Color(0xFF1D1B17)
    val t12 = Color(0xFF211F1B)
    val t17 = Color(0xFF2C2A26)
    val t20 = Color(0xFF32302C)
    val t22 = Color(0xFF373430)
    val t24 = Color(0xFF3B3935)
    val t30 = Color(0xFF494642)
    val t40 = Color(0xFF615E59)
    val t50 = Color(0xFF797772)
    val t60 = Color(0xFF93908B)
    val t70 = Color(0xFFAEABA6)
    val t80 = Color(0xFFC9C6C1)
    val t87 = Color(0xFFDDDAD4)
    val t90 = Color(0xFFE5E2DD)
    val t92 = Color(0xFFEBE8E2)
    val t94 = Color(0xFFF0EDE8)
    val t95 = Color(0xFFF3F0EB)
    val t96 = Color(0xFFF6F3EE)
    val t97 = Color(0xFFF9F6F1)
    val t98 = Color(0xFFFCF9F3)
    val t99 = Color(0xFFFFFCF6)
    val t100 = Color(0xFFFFFFFF)
}

/** 中性變體：比中性色多一點彩度，用在分隔與次要文字 */
internal object NV {
    val t0 = Color(0xFF000000)
    val t4 = Color(0xFF120E07)
    val t6 = Color(0xFF17130B)
    val t10 = Color(0xFF1F1B13)
    val t12 = Color(0xFF231F17)
    val t17 = Color(0xFF2E2921)
    val t20 = Color(0xFF343027)
    val t22 = Color(0xFF39342C)
    val t24 = Color(0xFF3D3930)
    val t30 = Color(0xFF4B463D)
    val t40 = Color(0xFF635E54)
    val t50 = Color(0xFF7C766D)
    val t60 = Color(0xFF969086)
    val t70 = Color(0xFFB0ABA0)
    val t80 = Color(0xFFCCC6BB)
    val t87 = Color(0xFFDFD9CE)
    val t90 = Color(0xFFE8E2D7)
    val t92 = Color(0xFFEDE7DD)
    val t94 = Color(0xFFF3EDE2)
    val t95 = Color(0xFFF6F0E5)
    val t96 = Color(0xFFF9F3E8)
    val t97 = Color(0xFFFCF6EB)
    val t98 = Color(0xFFFFF9EE)
    val t99 = Color(0xFFFFFCF6)
    val t100 = Color(0xFFFFFFFF)
}

/** 錯誤色。只用在刪除這類破壞性動作 */
internal object E {
    val t0 = Color(0xFF000000)
    val t4 = Color(0xFF250000)
    val t6 = Color(0xFF2E0000)
    val t10 = Color(0xFF3C0001)
    val t12 = Color(0xFF430001)
    val t17 = Color(0xFF560002)
    val t20 = Color(0xFF610003)
    val t22 = Color(0xFF690003)
    val t24 = Color(0xFF710004)
    val t30 = Color(0xFF890307)
    val t40 = Color(0xFFA62B25)
    val t50 = Color(0xFFC3483E)
    val t60 = Color(0xFFE26457)
    val t70 = Color(0xFFFF8072)
    val t80 = Color(0xFFFFAFA4)
    val t87 = Color(0xFFFFCCC5)
    val t90 = Color(0xFFFFD8D2)
    val t92 = Color(0xFFFFE0DB)
    val t94 = Color(0xFFFFE8E4)
    val t95 = Color(0xFFFFECE9)
    val t96 = Color(0xFFFFF0ED)
    val t97 = Color(0xFFFFF3F2)
    val t98 = Color(0xFFFFF7F6)
    val t99 = Color(0xFFFFFBFB)
    val t100 = Color(0xFFFFFFFF)
}