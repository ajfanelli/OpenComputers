package li.cil.oc.util.mods

import cpw.mods.fml.common.versioning.VersionParser
import cpw.mods.fml.common.{Loader, ModAPIManager}
import li.cil.oc.Settings

import scala.collection.mutable

object Mods {

  object IDs {
    final val AppliedEnergistics2 = "appliedenergistics2"
    final val BattleGear2 = "battlegear2"
    final val BuildCraftPower = "BuildCraftAPI|power"
    final val ComputerCraft = "ComputerCraft"
    final val CraftingCosts = "CraftingCosts"
    final val ElectricalAge = "Eln"
    final val Factorization = "factorization"
    final val ForgeMultipart = "ForgeMultipart"
    final val Galacticraft = "Galacticraft API"
    final val GregTech = "gregtech"
    final val IndustrialCraft2 = "IC2API"
    final val IndustrialCraft2Classic = "IC2-Classic"
    final val Mekanism = "Mekanism"
    final val MineFactoryReloaded = "MineFactoryReloaded"
    final val NotEnoughItems = "NotEnoughItems"
    final val PortalGun = "PortalGun"
    final val ProjectRedTransmission = "ProjRed|Transmission"
    final val RedLogic = "RedLogic"
    final val RedstoneFlux = "CoFHAPI|energy"
    final val StargateTech2 = "StargateTech2"
    final val ThermalExpansion = "ThermalExpansion"
    final val TinkersConstruct = "TConstruct"
    final val UniversalElectricity = "UniversalElectricity"
    final val VersionChecker = "VersionChecker"
    final val Waila = "Waila"
    final val WirelessRedstoneCBE = "WR-CBE|Core"
    final val WirelessRedstoneSV = "WirelessRedstoneCore"
  }

  private val knownMods = mutable.ArrayBuffer.empty[Mod]

  lazy val isPowerProvidingModPresent = knownMods.exists(mod => mod.providesPower && mod.isAvailable)

  val AppliedEnergistics2 = new SimpleMod(IDs.AppliedEnergistics2 + "@[rv1,)")
  val BattleGear2 = new SimpleMod(IDs.BattleGear2)
  val BuildCraftPower = new SimpleMod(IDs.BuildCraftPower, providesPower = true)
  val ComputerCraft = new SimpleMod(IDs.ComputerCraft)
  val CraftingCosts = new SimpleMod(IDs.CraftingCosts)
  val ElectricalAge = new SimpleMod(IDs.ElectricalAge)
  val Factorization = new SimpleMod(IDs.Factorization, providesPower = true)
  val ForgeMultipart = new SimpleMod(IDs.ForgeMultipart)
  val Galacticraft = new SimpleMod(IDs.Galacticraft, providesPower = true)
  val GregTech = new SimpleMod(IDs.GregTech)
  val IndustrialCraft2 = new ClassBasedMod(IDs.IndustrialCraft2,
    "ic2.api.energy.tile.IEnergySink",
    "ic2.api.energy.tile.IEnergyTile",
    "ic2.api.energy.event.EnergyTileLoadEvent",
    "ic2.api.energy.event.EnergyTileUnloadEvent")(providesPower = true)
  val IndustrialCraft2Classic = new SimpleMod(IDs.IndustrialCraft2Classic, providesPower = true)
  val Mekanism = new SimpleMod(IDs.Mekanism, providesPower = true)
  val MineFactoryReloaded = new SimpleMod(IDs.MineFactoryReloaded)
  val NotEnoughItems = new SimpleMod(IDs.NotEnoughItems)
  val PortalGun = new SimpleMod(IDs.PortalGun)
  val ProjectRedTransmission = new SimpleMod(IDs.ProjectRedTransmission)
  val RedLogic = new SimpleMod(IDs.RedLogic)
  val RedstoneFlux = new SimpleMod(IDs.RedstoneFlux, providesPower = true)
  val StargateTech2 = new Mod {
    def id = IDs.StargateTech2

    protected override lazy val isModAvailable = Loader.isModLoaded(IDs.StargateTech2) && {
      val mod = Loader.instance.getIndexedModList.get(IDs.StargateTech2)
      mod.getVersion.startsWith("0.7.")
    }
  }
  val ThermalExpansion = new SimpleMod(IDs.ThermalExpansion, providesPower = true)
  val TinkersConstruct = new SimpleMod(IDs.TinkersConstruct)
  val UniversalElectricity = new SimpleMod(IDs.UniversalElectricity, providesPower = true)
  val VersionChecker = new SimpleMod(IDs.VersionChecker)
  val Waila = new SimpleMod(IDs.Waila)
  val WirelessRedstoneCBE = new SimpleMod(IDs.WirelessRedstoneCBE)
  val WirelessRedstoneSV = new SimpleMod(IDs.WirelessRedstoneSV)

  trait Mod {
    knownMods += this

    private var powerDisabled = false

    protected lazy val isPowerModEnabled = !providesPower || (!Settings.get.pureIgnorePower && !Settings.get.powerModBlacklist.contains(id))

    protected def isModAvailable: Boolean

    def id: String

    def isAvailable = !powerDisabled && isModAvailable && isPowerModEnabled

    def providesPower: Boolean = false

    // This is called from the class transformer when injecting an interface of
    // this power type fails, to avoid class not found / class cast exceptions.
    def disablePower() = powerDisabled = true
  }

  class SimpleMod(val id: String, override val providesPower: Boolean = false) extends Mod {
    override protected lazy val isModAvailable = {
      val version = VersionParser.parseVersionReference(id)
      if (Loader.isModLoaded(version.getLabel))
        version.containsVersion(Loader.instance.getIndexedModList.get(version.getLabel).getProcessedVersion)
      else ModAPIManager.INSTANCE.hasAPI(version.getLabel)
    }
  }

  class ClassBasedMod(val id: String, val classNames: String*)(override val providesPower: Boolean) extends Mod {
    override protected lazy val isModAvailable = classNames.forall(className => try Class.forName(className) != null catch {
      case _: Throwable => false
    })
  }

}
