package com.routina.glance.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 從家族種子色 #2E6F6C 算出來的完整色階，不是手挑的幾個值。
 *
 * 產生方式：在 Oklch 裡固定色相與彩度、只變明度，超出 sRGB 就把彩度收回來。
 * 選 Oklch 不選 CIELAB 是因為 Lab 提亮時色相會跑掉，Oklch 在明度變化時守得住色相。
 * 色調編號沿用 M3 的定義（= CIE L*），所以下面的 t40 / t90 可以直接對上 M3 的角色表。
 *
 * **整套都要填滿**：Material 的 ColorScheme 只要有角色沒給值，就會沿用內建的紫色基線，
 * 於是分頁列、晶片、進度條軌道會冒出與家族色無關的紫（Fit v0.2.0 踩過）。
 */

/** 主色：Glance 的青綠 */
internal object P {
    val t0 = Color(0xFF000000)
    val t4 = Color(0xFF001211)
    val t6 = Color(0xFF001817)
    val t10 = Color(0xFF002120)
    val t12 = Color(0xFF002624)
    val t17 = Color(0xFF003130)
    val t20 = Color(0xFF003937)
    val t22 = Color(0xFF003E3C)
    val t24 = Color(0xFF004341)
    val t30 = Color(0xFF06524F)
    val t40 = Color(0xFF296A67)
    val t50 = Color(0xFF448480)
    val t60 = Color(0xFF5E9E9A)
    val t70 = Color(0xFF79B9B5)
    val t80 = Color(0xFF94D5D1)
    val t87 = Color(0xFFA7E9E4)
    val t90 = Color(0xFFAFF1ED)
    val t92 = Color(0xFFB5F7F3)
    val t94 = Color(0xFFBBFDF9)
    val t95 = Color(0xFFC0FFFB)
    val t96 = Color(0xFFCEFFFC)
    val t97 = Color(0xFFDBFFFC)
    val t98 = Color(0xFFE8FFFD)
    val t99 = Color(0xFFF4FFFE)
    val t100 = Color(0xFFFFFFFF)
}

/** 次要色：同色相、低彩度，用在不搶戲的容器 */
internal object S {
    val t0 = Color(0xFF000000)
    val t4 = Color(0xFF031110)
    val t6 = Color(0xFF071616)
    val t10 = Color(0xFF0F1F1E)
    val t12 = Color(0xFF132322)
    val t17 = Color(0xFF1D2E2D)
    val t20 = Color(0xFF233433)
    val t22 = Color(0xFF283937)
    val t24 = Color(0xFF2C3D3C)
    val t30 = Color(0xFF394B4A)
    val t40 = Color(0xFF516361)
    val t50 = Color(0xFF697B7A)
    val t60 = Color(0xFF829594)
    val t70 = Color(0xFF9CB0AE)
    val t80 = Color(0xFFB7CBCA)
    val t87 = Color(0xFFCADFDD)
    val t90 = Color(0xFFD3E8E6)
    val t92 = Color(0xFFD8EDEC)
    val t94 = Color(0xFFDEF3F1)
    val t95 = Color(0xFFE1F6F4)
    val t96 = Color(0xFFE4F9F7)
    val t97 = Color(0xFFE7FCFA)
    val t98 = Color(0xFFE9FFFD)
    val t99 = Color(0xFFF4FFFE)
    val t100 = Color(0xFFFFFFFF)
}

/** 第三色：色相 +60，只有少數強調處用得到 */
internal object T {
    val t0 = Color(0xFF000000)
    val t4 = Color(0xFF040F1C)
    val t6 = Color(0xFF081422)
    val t10 = Color(0xFF101D2A)
    val t12 = Color(0xFF13212F)
    val t17 = Color(0xFF1E2B3A)
    val t20 = Color(0xFF243241)
    val t22 = Color(0xFF283645)
    val t24 = Color(0xFF2C3B4A)
    val t30 = Color(0xFF3A4858)
    val t40 = Color(0xFF516071)
    val t50 = Color(0xFF69798A)
    val t60 = Color(0xFF8293A5)
    val t70 = Color(0xFF9CADC0)
    val t80 = Color(0xFFB7C9DC)
    val t87 = Color(0xFFCBDCF0)
    val t90 = Color(0xFFD3E5F8)
    val t92 = Color(0xFFD9EAFE)
    val t94 = Color(0xFFE2F0FF)
    val t95 = Color(0xFFE7F2FF)
    val t96 = Color(0xFFECF5FF)
    val t97 = Color(0xFFF0F7FF)
    val t98 = Color(0xFFF5FAFF)
    val t99 = Color(0xFFFAFCFF)
    val t100 = Color(0xFFFFFFFF)
}

/** 中性色：帶一點主色色相的灰，不是純灰 */
internal object N {
    val t0 = Color(0xFF000000)
    val t4 = Color(0xFF0C0F0F)
    val t6 = Color(0xFF111414)
    val t10 = Color(0xFF1A1C1C)
    val t12 = Color(0xFF1D2020)
    val t17 = Color(0xFF282B2A)
    val t20 = Color(0xFF2E3131)
    val t22 = Color(0xFF333535)
    val t24 = Color(0xFF373A3A)
    val t30 = Color(0xFF444747)
    val t40 = Color(0xFF5C5F5F)
    val t50 = Color(0xFF747877)
    val t60 = Color(0xFF8E9191)
    val t70 = Color(0xFFA8ACAC)
    val t80 = Color(0xFFC4C7C7)
    val t87 = Color(0xFFD7DBDB)
    val t90 = Color(0xFFE0E3E3)
    val t92 = Color(0xFFE5E9E9)
    val t94 = Color(0xFFEBEFEE)
    val t95 = Color(0xFFEEF2F1)
    val t96 = Color(0xFFF1F4F4)
    val t97 = Color(0xFFF4F7F7)
    val t98 = Color(0xFFF6FAFA)
    val t99 = Color(0xFFF9FDFD)
    val t100 = Color(0xFFFFFFFF)
}

/** 中性變體：比中性色多一點彩度，用在分隔與次要文字 */
internal object NV {
    val t0 = Color(0xFF000000)
    val t4 = Color(0xFF08100F)
    val t6 = Color(0xFF0D1515)
    val t10 = Color(0xFF151D1D)
    val t12 = Color(0xFF192121)
    val t17 = Color(0xFF232C2B)
    val t20 = Color(0xFF2A3232)
    val t22 = Color(0xFF2E3736)
    val t24 = Color(0xFF323B3B)
    val t30 = Color(0xFF404948)
    val t40 = Color(0xFF576160)
    val t50 = Color(0xFF6F7979)
    val t60 = Color(0xFF899392)
    val t70 = Color(0xFFA3AEAD)
    val t80 = Color(0xFFBEC9C8)
    val t87 = Color(0xFFD2DDDC)
    val t90 = Color(0xFFDAE5E4)
    val t92 = Color(0xFFE0EBEA)
    val t94 = Color(0xFFE5F1F0)
    val t95 = Color(0xFFE8F3F3)
    val t96 = Color(0xFFEBF6F5)
    val t97 = Color(0xFFEEF9F8)
    val t98 = Color(0xFFF1FCFB)
    val t99 = Color(0xFFF4FFFE)
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