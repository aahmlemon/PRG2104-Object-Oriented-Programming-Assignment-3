package ahmad.model

final class GameState(
                       val families: Vector[Family],
                       val farm:     Farm,
                       val grid:     Vector[CropTile],
                       val storage:  Storage,
                       val day:      Int,
                       val money:    Int
                     ) {

  def withFamilies(fs: Vector[Family]): GameState =
    new GameState(fs, farm, grid, storage, day, money)

  def withFarm(f: Farm): GameState =
    new GameState(families, f, grid, storage, day, money)

  def withGrid(g: Vector[CropTile]): GameState =
    new GameState(families, farm, g, storage, day, money)

  def withStorage(s: Storage): GameState =
    new GameState(families, farm, grid, s, day, money)

  def withDay(d: Int): GameState =
    new GameState(families, farm, grid, storage, d, money)

  def withMoney(m: Int): GameState =
    new GameState(families, farm, grid, storage, day, m)

  def updateFamilyAt(i: Int)(f: Family => Family): GameState =
    if (i < 0 || i >= families.length) this
    else withFamilies(families.updated(i, f(families(i))))

  def updateGridAt(i: Int)(g: CropTile => CropTile): GameState =
    if (i < 0 || i >= grid.length) this
    else withGrid(grid.updated(i, g(grid(i))))

  def addMoney(delta: Int): GameState  = withMoney(money + delta)
  
  def nextDay(): GameState             = withDay(day + 1)
}

object GameState {
  def apply(
             families: Vector[Family],
             farm:     Farm,
             grid:     Vector[CropTile],
             storage:  Storage,
             day:      Int,
             money:    Int
           ): GameState =
    new GameState(families, farm, grid, storage, day, money)
}
