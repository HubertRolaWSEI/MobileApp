package pl.wsei.pam.lab03

import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.ImageButton
import pl.wsei.pam.lab01.R
import java.util.Stack

class MemoryBoardView(
    private val gridLayout: GridLayout,
    private val cols: Int,
    private val rows: Int
) {
    private val tiles: MutableMap<String, Tile> = mutableMapOf()

    // Lista 18 wbudowanych ikon Androida (żebyś nie musiał tworzyć ich ręcznie)
    private val icons: List<Int> = listOf(
        android.R.drawable.ic_dialog_email,
        android.R.drawable.ic_dialog_info,
        android.R.drawable.ic_dialog_map,
        android.R.drawable.ic_menu_camera,
        android.R.drawable.ic_menu_call,
        android.R.drawable.ic_menu_compass,
        android.R.drawable.ic_menu_directions,
        android.R.drawable.ic_menu_gallery,
        android.R.drawable.ic_menu_help,
        android.R.drawable.ic_menu_info_details,
        android.R.drawable.ic_menu_mapmode,
        android.R.drawable.ic_menu_myplaces,
        android.R.drawable.ic_menu_preferences,
        android.R.drawable.ic_menu_recent_history,
        android.R.drawable.ic_menu_search,
        android.R.drawable.ic_menu_send,
        android.R.drawable.ic_menu_share,
        android.R.drawable.ic_menu_view
    )

    // Tył karty (zakryta) - używamy Twojej rakiety z Lab02
    private val deckResource: Int = R.drawable.baseline_rocket_launch_24

    private var onGameChangeStateListener: (MemoryGameEvent) -> Unit = {}
    private val matchedPair: Stack<Tile> = Stack()
    private var logic: MemoryGameLogic = MemoryGameLogic(cols * rows / 2)

    init {
        // Losowanie i tasowanie ikon dla par
        val shuffledIcons: MutableList<Int> = mutableListOf<Int>().also {
            it.addAll(icons.subList(0, cols * rows / 2))
            it.addAll(icons.subList(0, cols * rows / 2))
            it.shuffle()
        }

        // Pętla wygenerowana i przeniesiona z Aktywności
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                // Pobieramy i usuwamy pierwszy wylosowany obrazek z listy
                val currentIcon = shuffledIcons.removeAt(0)

                val btn = ImageButton(gridLayout.context).also {
                    it.tag = "${row}x${col}"
                    val layoutParams = GridLayout.LayoutParams()
                    layoutParams.width = 0
                    layoutParams.height = 0
                    layoutParams.setGravity(Gravity.CENTER)
                    layoutParams.columnSpec = GridLayout.spec(col, 1, 1f)
                    layoutParams.rowSpec = GridLayout.spec(row, 1, 1f)
                    it.layoutParams = layoutParams

                    gridLayout.addView(it)
                }

                // Tworzymy obiekt Tile i podpinamy go do logiki
                addTile(btn, currentIcon)
            }
        }
    }

    private fun onClickTile(v: View) {
        val tile = tiles[v.tag.toString()]
        if (tile != null) {
            matchedPair.push(tile)
            val matchResult = logic.process {
                tile.tileResource
            }
            onGameChangeStateListener(MemoryGameEvent(matchedPair.toList(), matchResult))
            if (matchResult != GameStates.Matching) {
                matchedPair.clear()
            }
        }
    }

    fun setOnGameChangeListener(listener: (event: MemoryGameEvent) -> Unit) {
        onGameChangeStateListener = listener
    }

    private fun addTile(button: ImageButton, resourceImage: Int) {
        button.setOnClickListener(::onClickTile)
        val tile = Tile(button, resourceImage, deckResource)
        tiles[button.tag.toString()] = tile
    }
    fun getState(): IntArray {
        // Tworzymy pustą tablicę o rozmiarze planszy
        val state = IntArray(cols * rows)
        var index = 0

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val tile = tiles["${row}x${col}"]
                // Jeśli karta jest odsłonięta, zapisujemy jej obrazek. Jeśli nie, zapisujemy -1.
                if (tile != null && tile.revealed) {
                    state[index] = tile.tileResource
                } else {
                    state[index] = -1
                }
                index++
            }
        }
        return state
    }

    fun setState(state: IntArray) {
        // 1. Sprawdzamy, które ikony zostały już odgadnięte
        val matchedIcons = state.filter { it != -1 }.distinct()

        // 2. Filtrujemy listę dostępnych ikon, odrzucając te już odgadnięte
        val availableIcons = icons.filterNot { it in matchedIcons }.toMutableList()

        // 3. Liczymy, ile par zostało nam jeszcze do ułożenia
        val neededPairsCount = (state.count { it == -1 }) / 2

        // Aktualizujemy logikę gry, żeby wiedziała ile par zostało do końca!
        logic = MemoryGameLogic(neededPairsCount)

        // 4. Losujemy NOWE ikony dla zakrytych kart
        val iconsToShuffle = mutableListOf<Int>()
        for (i in 0 until neededPairsCount) {
            val icon = availableIcons.removeAt(0)
            iconsToShuffle.add(icon)
            iconsToShuffle.add(icon)
        }
        iconsToShuffle.shuffle()

        // 5. Układamy karty na planszy
        var index = 0
        var shuffleIndex = 0
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val tile = tiles["${row}x${col}"]
                if (tile != null) {
                    val savedVal = state[index]
                    if (savedVal != -1) {
                        // Karta była odgadnięta -> Przywracamy ją!
                        tile.tileResource = savedVal
                        tile.revealed = true
                        tile.removeOnClickListener() // Blokujemy klikanie
                    } else {
                        // Karta była zakryta -> Przypisujemy jej nową wylosowaną ikonę
                        tile.tileResource = iconsToShuffle[shuffleIndex++]
                        tile.revealed = false
                    }
                }
                index++
            }
        }
    }
}