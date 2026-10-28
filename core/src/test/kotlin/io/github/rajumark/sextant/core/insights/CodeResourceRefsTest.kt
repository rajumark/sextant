package io.github.rajumark.sextant.core.insights

import io.github.rajumark.sextant.core.RawResource
import io.github.rajumark.sextant.core.model.ResourceRef
import io.github.rajumark.sextant.core.model.ResourceUseRecord
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CodeResourceRefsTest {
    private fun res(
        id: Int,
        type: String,
        name: String,
    ) = RawResource(id, type, name, "", null, null, emptyList())

    private val table =
        listOf(
            res(0x7f0b001c, "layout", "activity_main"),
            res(0x7f120003, "string", "app_name"),
            res(0x7f130001, "style", "Theme.App"),
        )

    private fun use(
        key: String,
        via: String? = null,
    ) = ResourceUseRecord("Lcom/app/Main;->onCreate(Landroid/os/Bundle;)V", key, via, 1, listOf(4))

    @Test
    fun `constants and R fields resolve to resource names, unknown ids are dropped`() {
        val refs =
            CodeResourceRefs.of(
                listOf(
                    use("0x7f0b001c", "Landroid/app/Activity;->setContentView(I)V"),
                    use("Lcom/app/R\$string;->app_name:I", "Landroid/content/Context;->getString(I)Ljava/lang/String;"),
                    use("Lcom/app/R\$style;->Theme_App:I"),
                    use("0x7f999999"),
                    use("Lcom/app/R\$string;->missing:I"),
                ),
                table,
            )
        refs shouldBe
            listOf(
                ResourceRef(
                    "@layout/activity_main",
                    "code",
                    "Lcom/app/Main;->onCreate(Landroid/os/Bundle;)V",
                    "Landroid/app/Activity;->setContentView(I)V",
                ),
                ResourceRef(
                    "@string/app_name",
                    "code",
                    "Lcom/app/Main;->onCreate(Landroid/os/Bundle;)V",
                    "Landroid/content/Context;->getString(I)Ljava/lang/String;",
                ),
                ResourceRef("@style/Theme.App", "code", "Lcom/app/Main;->onCreate(Landroid/os/Bundle;)V", ""),
            )
    }
}
