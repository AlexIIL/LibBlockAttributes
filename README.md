# LibBlockAttributes

This is a library mod for the [Fabric](https://fabricmc.net/) API, based around [Minecraft](https://minecraft.net).

This project has 3 major concepts: attributes, item management, and fluid management.

### Attributes

Unfortunately this part of the API hasn't been very thoroughly thought out :(

An "attribute" is just a class that has an instance of "alexiil.mc.lib.attributes.Attribute" stored in a public static final field.

A block can provide attribute instances by implementing IAttributeBlock and then offering instances like this:

```java
public class MyBlock extends Block implements IAttributeBlock {
    @Override
    public void addAllAttributes(World world, BlockPos pos, BlockState state, AttributeList<?> to) {
        to.offer(an_attribute_instance);
    }
}
```

A block can request instances of a specific attribute by:

1. Finding the appropriate `public static final Attribute<>` field.
    - This defines item attributes in "ItemAttributes", and fluid attributes in "FluidAttributes"
2. Calling either "getAll", "getFirstOrNull", "getFirst", or "get" and passing in the appropriate world, position, and SearchParameter.

For example a hopper-like block might call this:

```java
/**
 * Finds an {@link IItemInsertable} in the given direction.
 *
 * @param world The world object to search within
 * @param thisPos The position to search from.
 * @param direction The direction to search in.
 * @return An {@link IItemInsertable} that we can attempt to insert into.
 */
public static IItemInsertable getNeighbourInsertable(World world, BlockPos thisPos, Direction direction) {

    BlockPos offsetPos = thisPos.offset(direction);

    // Note that the direction is always the direction of the search:
    // so it's always from the hopper to it's neighbour, and not the
    // side of the neighbour to search.
    SearchParameter searchParam = SearchParamDirectional.of(direction);

    // Note that this object will never be null - it will just return
    // a "rejecting" item insertable that never allows anything to be
    // inserted if there's no valid item insertable there. 
    return ItemAttributes.INSERTABLE.get(world, offsetPos, searchParam);
}
```

You can create custom attributes by calling one of the `create` methods in `Attributes`.
Note that attribute instances are never registered anywhere so you should store the
attribute in a `public static final Attribute<MyAttributeClass>` field.

In addition there are three type of attribute:

- Attribute: The base type, implies nothing about attribute instances but only provides the `getAll()` and `getFirstOrNull()` accessor methods.
- AttributeDefaulted: This implies that it is always possible to have a default instance which can used safely everywhere that normal instances could be. This also provides the `getFirst()` accessor method.
- AttributeCombinable: (extends AttributeDefaulted) This implies that it is always possible to combine multiple attribute instances and use them safely everywhere that normal instances can be. This provides the `get()` method which returns a combined version of `getAll()`.

For example a custom attribute based around a fictional class `RobotDockingPort` (which could be used by buildcraft robots) would be defined like this:

```java
public static final Attribute<RobotDockingPort> DOCKING_PORT = Attributes.create(RobotDockingPort.class);
```

Alternatively you can look inside `FluidAttributes` for an example involving combinable attributes.

### Item Management

Everything in the "alexiil.mc.lib.attributes.item" package is dedicated to item management, and is intended for any mod to use.

The core API's are:

- IFixedItemInv* (A version of minecraft's Inventory class that only deals with indexed item slot accessors and modification).
- IFixedItemInvView* (A read-only version of IFixedItemInv).
- IItemInvStats* (containing statistics for any inventory).
- IItemInsertable* (containing item insertion)
- IItemExtractable* (containing item extraction)
- IItemFilter (a Predicate for ItemStacks)
- IItemInvSlotChangeListener (for listening to changes in an IFixedItemInvView - although not all implementations will support this).

(*Is an attribute by default)

In addition there are various utility classes:

- ItemAttributes (containing all of the item attributes)
- ItemInvUtil (for accessing attribute versions of the above API's, and a "move" method for moving items from an IItemExtractable to an IItemInsertable).
- ItemStackUtil (containing general methods around single ItemStack instances that were needed to create this API).
- ItemStackCollections (utility methods for creating Sets and Maps that compare ItemStack's correctly (as ItemStack itself doesn't override equals() or hashCode()).

The "impl" subpackage is also for public use, and it contains a lot of concrete implementations for the core API's.

### Fluid Management

Everything in the "alexiil.mc.lib.attributes.fluid" package is dedicated to fluid management, and is intended for any mod to use. This is heavily based on the item management API, but with a few core changes:

- Instead of ItemStack we have FluidVolume (with an amount) and FluidKey (without an amount)
- Slots are called "tanks"
- Tanks/Slots do not have a pre-defined maximum amount (and neither do fluids themselves as that depends wholly on the container).
- The units for fluids are based around drips, bottles, and buckets, however custom fluids can use their own units if that makes more sense than the default (buckets).

The core API's are:

- IFixedFluidInv*.
- IFixedFluidInvView* (A read-only version of IFixedFluidInv).
- IFluidInvStats* (containing statistics for any fluid inventory).
- IFluidInsertable* (containing fluid insertion)
- IFluidExtractable* (containing fluid extraction)
- IFluidFilter (a Predicate for FluidKey's)
- IFluidInvTankChangeListener (for listening to changes in an IFixedItemInvView - although not all implementations will support this).

(*Is an attribute by default)

In addition there are two utility classes:

- FluidAttributes (containing all of the fluid attributes mentioned above)
- FluidVolumeUtil (containing general methods around FluidVulume instances that were needed to create this API).

The "impl" subpackage is also for public use, and it contains a lot of concrete implementations for the core API's.

### Maven

Currently you can use this by adding this to your build.gradle:

```
repositories {
    maven {
        name = "BuildCraft"
        url = "https://mod-buildcraft.com/maven"
    }
}

dependencies {
    modCompile "alexiil.mc.lib:libblockattributes:0.2.0"
}
```
