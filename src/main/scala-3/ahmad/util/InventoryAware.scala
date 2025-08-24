package ahmad.util

trait InventoryAware {
  def setOnInventoryChanged(cb: () => Unit): Unit
}
