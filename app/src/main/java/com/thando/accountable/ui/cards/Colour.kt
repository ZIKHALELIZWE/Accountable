package com.thando.accountable.ui.cards

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.thando.accountable.R
import com.thando.accountable.recyclerviewadapters.ColourItemAdapter
import com.thando.accountable.ui.decoration.GridSpacingItemDecoration

data class Colour(
    val colour: Int
)
{
    var backgroundDrawable: Drawable? = null

    fun setDrawable(context: Context){
        backgroundDrawable = ContextCompat.getDrawable(context, R.drawable.square_rounded_corners)?.mutate()
        backgroundDrawable?.colorFilter = PorterDuffColorFilter(colour, PorterDuff.Mode.SRC_IN)
    }

    fun removeDrawable(){
        backgroundDrawable = null
    }

    companion object{
        fun showColorPickerDialog(
            context: Context, processSelectedColour:(selectedColour:Int)->Unit
        ) {
            lateinit var colorPickerDialog: AlertDialog
            val density = context.resources.displayMetrics.density
            val colors = listOf(
                Color.BLACK, Color.WHITE, Color.CYAN, Color.rgb(179, 157, 219), Color.MAGENTA, Color.rgb(245, 245, 220), Color.YELLOW,
                Color.rgb(169, 169, 169), Color.GREEN, Color.rgb(244, 164, 96), Color.BLUE, Color.RED,
                Color.rgb(255, 228, 181), Color.rgb(72, 61, 139), Color.rgb(205, 92, 92), Color.rgb(255, 165, 0), Color.rgb(102, 205, 170)
            )

            val colourList = ArrayList<Colour>()
            colors.forEach{colourList.add(Colour(it))}

            val numColumns = 5 // Desired number of columns
            val padding = dpToPx(15, density) // Convert 15 dp to pixels
            val spacing = dpToPx(15, density) // Set the spacing between items in dp

            val recyclerView = RecyclerView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                layoutManager = GridLayoutManager(context, numColumns)
                setPadding(padding, dpToPx(20, density), padding, padding) // Convert padding to pixels
                val colourItemAdapter = ColourItemAdapter(context) { selectedColor ->
                    // Do something with the selected color
                    processSelectedColour(selectedColor)
                    colorPickerDialog.dismiss()
                }
                adapter = colourItemAdapter
                addItemDecoration(GridSpacingItemDecoration(numColumns, spacing, true))
                colourItemAdapter.submitList(colourList.toMutableList())
            }

            colorPickerDialog = AlertDialog.Builder(context, R.style.CustomAlertDialogTheme)
                .setTitle(context.getString(R.string.choose_a_colour))
                .setView(recyclerView)
                .setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
            colorPickerDialog.show()
        }

        private fun dpToPx(dp: Int, density:Float): Int {
            return (dp * density).toInt()
        }
    }
}


