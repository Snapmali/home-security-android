package com.snapkirin.homesecurity.ui.util

import com.drakeet.multitype.MultiTypeAdapter
import com.snapkirin.homesecurity.ui.main.model.ItemListChangeEvent
import com.snapkirin.homesecurity.ui.main.model.PullingItemsRequestResult

open class BaseItemListViewModel: BaseNetworkRequestViewModel() {

    protected fun itemListChanged(changeEvent: ItemListChangeEvent, adapter: MultiTypeAdapter) {
        when (changeEvent.event) {
            ItemListChangeEvent.ADD -> {
                adapter.notifyItemRangeInserted(changeEvent.index, changeEvent.count)
            }
            ItemListChangeEvent.DELETE -> {
                adapter.notifyItemRangeRemoved(changeEvent.index, changeEvent.count)
            }
            ItemListChangeEvent.UPDATE -> {
                adapter.notifyItemRangeChanged(changeEvent.index, changeEvent.count)
            }
        }
    }

    protected fun pullingItemsFailureHandler(
        code: Int,
        itemCategory: Int,
        requestMode: Int
    ): PullingItemsRequestResult{
        val baseResult = requestFailureHandler(code)
        return PullingItemsRequestResult(
            false,
            code,
            baseResult.stringId,
            itemCategory,
            requestMode,
            loginNeeded = baseResult.loginNeeded
        )
    }
}