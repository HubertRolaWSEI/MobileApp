package pl.wsei.pam.lab03

enum class GameStates {
    Matching, Match, NoMatch, Finished
}

data class MemoryGameEvent(
    val tiles: List<Tile>,
    val state: GameStates
)

class MemoryGameLogic(private val maxMatches: Int) {
    private var valueFunctions: MutableList<() -> Int> = mutableListOf()
    private var matches: Int = 0

    fun process(value: () -> Int): GameStates {
        if (valueFunctions.size < 1) {
            valueFunctions.add(value)
            return GameStates.Matching
        }
        valueFunctions.add(value)
        val result = valueFunctions[0]() == valueFunctions[1]()
        matches += if (result) 1 else 0
        valueFunctions.clear()

        return when (result) {
            true -> if (matches == maxMatches) GameStates.Finished else GameStates.Match
            false -> GameStates.NoMatch
        }
    }
}