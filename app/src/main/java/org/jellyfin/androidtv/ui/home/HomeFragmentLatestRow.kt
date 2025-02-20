package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.leanback.widget.ArrayObjectAdapter
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.constant.ChangeTriggerType
import org.jellyfin.androidtv.ui.browsing.BrowseRowDef
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.apiclient.model.querying.ItemFields
import org.jellyfin.apiclient.model.querying.ItemsResult
import org.jellyfin.apiclient.model.querying.LatestItemsQuery

class HomeFragmentLatestRow(
	private val context: Context,
	private val userRepository: UserRepository,
	private val views: ItemsResult
) : HomeFragmentRow {
	override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: ArrayObjectAdapter) {
		// Get configuration (to find excluded items)
		val configuration = userRepository.currentUser.value?.configuration

		// Create a list of views to include
		val latestItemsExcludes = configuration?.latestItemsExcludes.orEmpty()
		views.items
			.filterNot { item -> item.collectionType in EXCLUDED_COLLECTION_TYPES || item.id in latestItemsExcludes }
			.map { item ->
				// Create query and add it to a new row
				val query = LatestItemsQuery().apply {
					fields = arrayOf(
						ItemFields.PrimaryImageAspectRatio,
						ItemFields.Overview,
						ItemFields.ChildCount
					)
					imageTypeLimit = 1
					parentId = item.id
					groupItems = true
					limit = ITEM_LIMIT
				}

				val title = String.format("%s %s", context.getString(R.string.lbl_latest), item.name)
				HomeFragmentBrowseRowDefRow(BrowseRowDef(title, query, arrayOf(ChangeTriggerType.LibraryUpdated)))
			}.forEach { row ->
				// Add row to adapter
				row.addToRowsAdapter(context, cardPresenter, rowsAdapter)
			}
	}

	companion object {
		// Collections exclused from latest row based on app support and common sense
		private val EXCLUDED_COLLECTION_TYPES = arrayOf("playlists", "livetv", "boxsets", "channels", "books")

		// Maximum ammount of items loaded for a row
		private const val ITEM_LIMIT = 50
	}
}
