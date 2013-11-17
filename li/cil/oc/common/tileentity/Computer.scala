package li.cil.oc.common.tileentity

import li.cil.oc.Config
import li.cil.oc.api.network._
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.server.{PacketSender => ServerPacketSender, driver, component}
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeDirection
import scala.Some

abstract class Computer(isRemote: Boolean) extends Environment with ComponentInventory with Rotatable with Redstone with Analyzable {
  val computer = if (isRemote) null else new component.Computer(this)

  def node = if (isClient) null else computer.node

  override def isClient = computer == null

  private var isRunning = false

  private var hasChanged = false

  // ----------------------------------------------------------------------- //

  def isOn = isRunning

  def isOn_=(value: Boolean) = {
    isRunning = value
    world.markBlockForRenderUpdate(x, y, z)
    this
  }

  def markAsChanged() = hasChanged = true

  def hasRedstoneCard = items.exists {
    case Some(item) => driver.item.RedstoneCard.worksWith(item)
    case _ => false
  }

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    if (isServer) {
      // If we're not yet in a network we were just loaded from disk. We skip
      // the update this round to allow other tile entities to join the network,
      // too, avoiding issues of missing nodes (e.g. in the GPU which would
      // otherwise loose track of its screen).
      if (node != null && node.network != null) {
        computer.update()
      }

      if (hasChanged) {
        hasChanged = false
        worldObj.markTileEntityChunkModified(xCoord, yCoord, zCoord, this)
      }

      if (isRunning != computer.isRunning) {
        isOutputEnabled = hasRedstoneCard && computer.isRunning
        ServerPacketSender.sendComputerState(this, computer.isRunning)
      }
      isRunning = computer.isRunning

      updateRedstoneInput()

      for (component <- components) component match {
        case Some(environment) => environment.update()
        case _ => // Empty.
      }
    }

    super.updateEntity()
  }

  override def validate() = {
    super.validate()
    if (isClient) {
      ClientPacketSender.sendRotatableStateRequest(this)
      ClientPacketSender.sendComputerStateRequest(this)
      ClientPacketSender.sendRedstoneStateRequest(this)
    }
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    if (isServer) {
      computer.load(nbt.getCompoundTag(Config.namespace + "computer"))
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    if (isServer) {
      nbt.setNewCompoundTag(Config.namespace + "computer", computer.save)
    }
  }

  // ----------------------------------------------------------------------- //

  override def onInventoryChanged() {
    super.onInventoryChanged()
    if (isServer) {
      computer.recomputeMemory()
      isOutputEnabled = hasRedstoneCard && computer.isRunning
    }
  }

  override protected def onRotationChanged() {
    super.onRotationChanged()
    checkRedstoneInputChanged()
  }

  override protected def onRedstoneInputChanged(side: ForgeDirection) {
    super.onRedstoneInputChanged(side)
    if (isServer) {
      computer.signal("redstone_changed", Int.box(side.ordinal()))
    }
  }

  // ----------------------------------------------------------------------- //

  def onAnalyze(stats: NBTTagCompound, player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
    if (computer != null) computer.lastError match {
      case Some(value) => stats.setString(Config.namespace + "text.Analyzer.LastError", value)
      case _ =>
    }
    computer
  }
}
