/** The base package for {@link net.minecraft.fluid.Fluid fluid} management.
 * <p>
 * As vanilla minecraft doesn't use fluids as much as items there are a few key differences between how this package
 * works verses {@link alexiil.mc.lib.attributes.item}:
 * <ul>
 * <li>Instead of {@link net.minecraft.item.ItemStack ItemStack} we have
 * {@link alexiil.mc.lib.attributes.fluid.volume.FluidVolume FluidVolume} (with an amount) and
 * {@link alexiil.mc.lib.attributes.fluid.volume.FluidKey FluidKey} (without an amount)</li>
 * <li>Slots are called "tanks"</li>
 * <li>Tanks/Slots do not have a pre-defined maximum amount (and neither do fluids themselves as that depends wholly on
 * the container).</li>
 * <li>The units for fluids are based around {@link alexiil.mc.lib.attributes.fluid.volume.FluidVolume#BASE_UNIT drips},
 * {@link alexiil.mc.lib.attributes.fluid.volume.FluidVolume#NUGGET nuggets},
 * {@link alexiil.mc.lib.attributes.fluid.volume.FluidVolume#INGOT ingots}, and
 * {@link alexiil.mc.lib.attributes.fluid.volume.FluidVolume#BLOCK blocks}.</li>
 * </ul>
 */
package alexiil.mc.lib.attributes.fluid;
