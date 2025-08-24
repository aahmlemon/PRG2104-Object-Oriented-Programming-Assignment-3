package ahmad.service

import ahmad.model.*
import ahmad.service.SaveLoadService.SaveLoadCodecs.given
import upickle.default.*

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.util.Try

/**
 * Save/Load support for the game.
 *
 * Design:
 *  - We serialize a *snapshot DTO* (plain, JSON-friendly structures) rather than
 *    the runtime domain objects directly.
 *  - Domain ↔ DTO mapping is explicit (see `toDTO` / `fromDTO`) to keep the
 *    persistence layer decoupled from internal representations (e.g., case class → non-case).
 *  - `Produce` is persisted by *string id* using `p.name`. If names change, adjust
 *    `idOf` / `produceOf` to keep backward compatibility.
 *
 * JSON codec:
 *  - Uses uPickle `ReadWriter` derivation for DTOs only.
 *
 * Public API:
 *  - `save(path, gs, market): Try[Unit]`
 *  - `load(path): Try[(GameState, MarketService)]`
 */
object SaveLoadService {
  /**
   * All uPickle `ReadWriter`s for the DTO types.
   * Kept in a nested object to avoid polluting other imports by accident.
   */
  object SaveLoadCodecs {
    given ReadWriter[NutritionDTO]    = macroRW
    given ReadWriter[FamilyDTO]       = macroRW
    given ReadWriter[CropTileDTO]     = macroRW
    given ReadWriter[StorageDTO]      = macroRW
    given ReadWriter[MarketDTO]       = macroRW
    given ReadWriter[FarmDTO]         = macroRW
    given ReadWriter[GameSnapshotDTO] = macroRW
  }

  /** Persist a Produce as a stable string id. Currently uses name. */
  private def idOf(p: Produce): String = p.name

  /** Recreate a Produce from its persisted id. */
  private def produceOf(id: String): Produce = id match
    case "Rice"       => Rice()
    case "Beans"      => Beans()
    case "Vegetables" => Vegetables()
    case _            => Rice() // Fallback: prevents load failures on unknown ids
  
  /**
   * Build a portable DTO snapshot from the live game state + market.
   *
   * Notes:
   *  - Families: persist `name`, `dailyNeed`, and `assigned` map (Produce→qty) via ids.
   *  - Grid: persist each tile's seed (by id) and days grown.
   *  - Storage: persist stock map (Produce→qty) via ids.
   *  - Farm: persist upgrade levels explicitly.
   *  - Market: persist price map (Produce→price) via ids.
   */
  private def toDTO(gs: GameState, market: MarketService): GameSnapshotDTO = {
    val fams = gs.families.map { f =>
      val dn = f.dailyNeed
      FamilyDTO(
        name = f.name,
        dailyNeed = NutritionDTO(dn.cal, dn.protein, dn.carbs, dn.vitamins),
        assigned = f.assigned.map { case (p, q) => idOf(p) -> q }
      )
    }

    val grid = gs.grid.map { t =>
      CropTileDTO(
        seed      = t.seed.map(idOf),
        daysGrown = t.daysGrown
      )
    }

    val storage = StorageDTO(
      stock = gs.storage.getStock.map { case (p, q) => idOf(p) -> q }
    )

    val farm = FarmDTO(
      irrigationLevel = gs.farm.irrigationLevel,
      yieldLevel      = gs.farm.yieldLevel
    )

    val marketD = MarketDTO(
      prices = market.prices.map { case (p, price) => idOf(p) -> price }
    )

    GameSnapshotDTO(
      day     = gs.day,
      money   = gs.money,
      families= fams,
      grid    = grid,
      storage = storage,
      farm    = farm,
      market  = marketD
    )
  }

  /**
   * Rehydrate a live `GameState` and `MarketService` from a snapshot DTO.
   *
   * Assumptions:
   *  - Unknown produce ids fall back to `Rice()` to avoid hard load failures.
   *  - Non-case classes are constructed explicitly.
   */
  private def fromDTO(dto: GameSnapshotDTO): (GameState, MarketService) = {
    val fams = dto.families.map { f =>
      val n = f.dailyNeed
      new Family(
        name       = f.name,
        dailyNeed  = new Nutrition(n.cal, n.protein, n.carbs, n.vitamins),
        assigned   = f.assigned.map { case (id, q) => produceOf(id) -> q }
      )
    }.toVector

    val grid = dto.grid.map { t =>
      new CropTile(
        seed      = t.seed.map(produceOf),
        daysGrown = t.daysGrown
      )
    }.toVector

    val storage = Storage(
      stock = dto.storage.stock.map { case (id, q) => produceOf(id) -> q }
    )

    val farm = new Farm(
      irrigationLevel = dto.farm.irrigationLevel,
      yieldLevel      = dto.farm.yieldLevel
    )

    val gs = new GameState(
      families = fams,
      farm     = farm,
      grid     = grid,
      storage  = storage,
      day      = dto.day,
      money    = dto.money
    )

    // Restore market directly from persisted prices (keeps day-to-day variability outside)
    val market: MarketService =
      MarketService.fromPrices(dto.market.prices.map { case (id, p) => produceOf(id) -> p })

    (gs, market)
  }

  /**
   * Serialize and write the snapshot to disk as pretty-printed JSON.
   *
   * @param path   file path to write
   * @param gs     game state to serialize
   * @param market market prices to serialize
   * @return       Try[Unit] with any I/O or serialization error
   */
  def save(path: Path, gs: GameState, market: MarketService): Try[Unit] = Try {
    val json = write(toDTO(gs, market), indent = 2)        // pretty for easy diffing
    Files.writeString(path, json, StandardCharsets.UTF_8)  // atomicity not guaranteed; keep simple
    ()
  }

  /**
   * Read and deserialize a snapshot from disk.
   *
   * @param path path of the JSON file
   * @return     Try[(GameState, MarketService)]
   */
  def load(path: Path): Try[(GameState, MarketService)] = Try {
    val json = Files.readString(path, StandardCharsets.UTF_8)
    val dto  = read[GameSnapshotDTO](json)
    fromDTO(dto)
  }
}
