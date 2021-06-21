package com.snapkirin.homesecurity.ui.main.model

data class ItemListChangeEvent (
        val event: Int,
        val index: Int,
        var count: Int = 1,
        var target: Int = 0
        ) {

        companion object {
                const val ADD = 1
                const val DELETE = 2
                const val UPDATE = 3
        }
}