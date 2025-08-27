package ahmad.service

final case class NutritionDTO(cal: Int, protein: Int, carbs: Int, vitamins: Int)
final case class CropTileDTO(seed: Option[String], daysGrown: Int)
final case class StorageDTO(stock: Map[String, Int])
final case class MarketDTO(prices: Map[String, Int])
final case class FarmDTO(irrigationLevel: Int, yieldLevel: Int)

final case class GameSnapshotDTO(
                                  day: Int,
                                  money: Int,
                                  families: Seq[FamilyDTO],
                                  grid: Seq[CropTileDTO],
                                  storage: StorageDTO,
                                  farm: FarmDTO,
                                  market: MarketDTO
                                )

final case class FamilyDTO(
                            name: String,
                            dailyNeed: NutritionDTO,
                            assigned: Map[String, Int],
                            stockpile: Option[NutritionDTO] = None
                          )
