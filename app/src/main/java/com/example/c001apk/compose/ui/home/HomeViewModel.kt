package com.example.c001apk.compose.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c001apk.compose.logic.model.HomeMenu
import com.example.c001apk.compose.logic.repository.HomeMenuRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeMenuRepo: HomeMenuRepo,
) : ViewModel() {

    val homeMenus: Flow<List<HomeMenu>> = homeMenuRepo.loadAllListFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            homeMenuRepo.upsertList(normalizeMenus(homeMenuRepo.loadAllList()))
        }
    }

    fun setTabs(orderedTabs: List<TabType>, enabledTabs: Set<TabType>) {
        if (enabledTabs.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val storedByTitle = normalizeMenus(homeMenuRepo.loadAllList())
                .associateBy(HomeMenu::title)
            val completeOrder = orderedTabs.distinct() + TabType.entries.filterNot(orderedTabs::contains)
            val menus = completeOrder.mapIndexed { index, type ->
                storedByTitle[type.name]?.copy(
                    position = index,
                    isEnable = type in enabledTabs,
                ) ?: HomeMenu(
                    position = index,
                    title = type.name,
                    isEnable = type in enabledTabs,
                )
            }
            homeMenuRepo.upsertList(menus)
        }
    }

    private fun normalizeMenus(storedMenus: List<HomeMenu>): List<HomeMenu> {
        val storedByType = storedMenus
            .sortedBy(HomeMenu::position)
            .mapNotNull { menu ->
                runCatching { TabType.valueOf(menu.title) }.getOrNull()?.let { it to menu }
            }
            .distinctBy { it.first }
        val storedTypes = storedByType.mapTo(mutableSetOf()) { it.first }
        val orderedTypes = storedByType.map { it.first } + TabType.entries.filterNot(storedTypes::contains)
        val storedMenusByType = storedByType.toMap()

        return orderedTypes.mapIndexed { index, type ->
            storedMenusByType[type]?.copy(position = index, title = type.name)
                ?: HomeMenu(position = index, title = type.name, isEnable = true)
        }
    }
}
