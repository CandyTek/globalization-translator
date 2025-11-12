package com.wilinz.globalization.translator.action.properties

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.wilinz.globalization.translator.i18n.message
import org.codejive.properties.Properties
// 1. (新增) 导入标准的 java.util.Properties 并使用别名
import java.util.Properties as JavaProperties

class TranslateAction : AnAction() {
	override fun getActionUpdateThread(): ActionUpdateThread {
		return ActionUpdateThread.BGT
	}
	override fun actionPerformed(e: AnActionEvent) {
		val file = CommonDataKeys.VIRTUAL_FILE.getData(e.dataContext) ?: return
		propertiesActionPerformed(
			e = e,
			file = file,
			// 2. (修改) 替换 getProperties 的实现
			getProperties = { charset ->
				// 使用标准、内存安全的 Properties 类来加载
				val javaProps = JavaProperties()
				file.inputStream.use { stream ->
					javaProps.load(stream.reader(charset))
				}

				// 创建你的库所期望的 Properties 对象
				val codejiveProps = Properties()

				// 安全地将所有属性复制过去
				javaProps.stringPropertyNames().forEach { key ->
					codejiveProps.put(key, javaProps.getProperty(key))
				}

				// 返回填充好的、安全的 codejiveProps
				codejiveProps
			},
			isShowOverwriteCheckBox = true
		)
	}

	override fun update(e: AnActionEvent) {
		super.update(e)
		e.presentation.text = message("translate_this_file")
		val file = CommonDataKeys.VIRTUAL_FILE.getData(e.dataContext) ?: return
		val isSelectValueFile = file.extension == "properties"
		e.presentation.isEnabledAndVisible = isSelectValueFile
	}
}
