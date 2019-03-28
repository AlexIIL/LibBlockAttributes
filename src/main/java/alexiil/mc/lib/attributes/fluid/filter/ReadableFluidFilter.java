package alexiil.mc.lib.attributes.fluid.filter;

/** Marker interface for {@link IFluidFilter} that indicates that object obtaining instances of this might be able to
 * read the real contents.
 * <p>
 * NOTE: This can only be implemented by classes <strong>included in LibBlockAttributes!</strong>. (As many
 * implementations must implement direct support for subclasses of this). */
public interface IReadableFluidFilter extends IFluidFilter {

    public static void checkValidity(IReadableFluidFilter filter) {
        String clsName = IReadableFluidFilter.class.getName();
        String expectedPackage = clsName.substring(0, clsName.lastIndexOf('.'));
        if (!filter.getClass().getName().startsWith(expectedPackage)) {
            throw new IllegalStateException(
                "The owner of " + filter.getClass() + " has incorrectly implemented IReadableFluidFilter!\n"
                    + "Note that only LibBlockAttributes should define readable fluid filters, "
                    + "as otherwise there's no way to guarentee compatibility!");
        }
    }
}
