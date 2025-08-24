package ahmad.service

import ahmad.model.*
import ahmad.service.SaveLoadService.SaveLoadCodecs.given
import upickle.default.*

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.util.{Failure, Success, Try}

object SaveLoadService {

  object SaveLoadCodecs {
    given ReadWriter[NutritionDTO]    = macroRW
    given ReadWriter[FamilyDTO]       = macroRW
    given ReadWriter[CropTileDTO]     = macroRW
    given ReadWriter[StorageDTO]      = macroRW
    given ReadWriter[MarketDTO]       = macroRW
    given ReadWriter[FarmDTO]         = macroRW
    given ReadWriter[GameSnapshotDTO] = macroRW
  }
  
  private def idOf(p: Produce): String = p.name
  private def produceOf(id: String): Produce = id match
    case "Rice"       => Rice()
    case "Beans"      => Beans()
    case "Vegetables" => Vegetables()
    case _            => Rice() // fallback
  
  private def toDTO(gs: GameState, market: MarketService): GameSnapshotDTO = {
    val fams = gs.families.map { f =>
      val dn = f.dailyNeed
      FamilyDTO(
        f.name,
        NutritionDTO(dn.cal, dn.protein, dn.carbs, dn.vitamins),
        f.assigned.map { case (p, q) => idOf(p) -> q }
      )
    }
    val grid = gs.grid.map { t =>
      CropTileDTO(t.seed.map(idOf), t.daysGrown)
    }
    val storage = StorageDTO(gs.storage.getStock.map { case (p, q) => idOf(p) -> q })
    val farm    = FarmDTO(gs.farm.irrigationLevel, gs.farm.yieldLevel)
    val marketD = MarketDTO(market.prices.map { case (p, price) => idOf(p) -> price })

    GameSnapshotDTO(gs.day, gs.money, fams, grid, storage, farm, marketD)
  }
  
  private def fromDTO(dto: GameSnapshotDTO): (GameState, MarketService) = {
    val fams = dto.families.map { f =>
      val n = f.dailyNeed
      new Family(
        name = f.name,
        dailyNeed = new Nutrition(n.cal, n.protein, n.carbs, n.vitamins),
        assigned = f.assigned.map { case (id, q) => produceOf(id) -> q }
      )
    }.toVector

    val grid = dto.grid.map { t =>
      new CropTile(seed = t.seed.map(produceOf), daysGrown = t.daysGrown)
    }.toVector

    val storage = Storage(dto.storage.stock.map { case (id, q) => produceOf(id) -> q })
    val farm    = new Farm(dto.farm.irrigationLevel, dto.farm.yieldLevel)

    val gs = new GameState(
      families = fams,
      farm     = farm,
      grid     = grid,
      storage  = storage,
      day      = dto.day,
      money    = dto.money
    )
    
    val market: MarketService = MarketService.fromPrices(dto.market.prices.map { case (id, p) => produceOf(id) -> p })
    (gs, market)
  }

  // --- Public API ---
  def save(path: Path, gs: GameState, market: MarketService): Try[Unit] = Try {
    val json = write(toDTO(gs, market), indent = 2)
    Files.writeString(path, json, StandardCharsets.UTF_8)
    ()
  }

  def load(path: Path): Try[(GameState, MarketService)] = Try {
    val json = Files.readString(path, StandardCharsets.UTF_8)
    val dto  = read[GameSnapshotDTO](json)
    fromDTO(dto)
  }
}
