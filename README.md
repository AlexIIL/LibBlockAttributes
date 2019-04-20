# LibBlockAttributes

This is a library mod for the [Fabric](https://fabricmc.net/) API, based around [Minecraft](https://minecraft.net).

This project has 3 major concepts: attributes, item management, and fluid management.

### Attributes

An "attribute" is just a class that has an instance of "alexiil.mc.lib.attributes.Attribute" stored in a public static final field.

A block can provide attribute instances by implementing AttributeProviderBlock and then offering instances like this:

```java
public class MyBlock extends Block implements AttributeProviderBlock {
    @Override
    public void addAllAttributes(World world, BlockPos pos, BlockState state, AttributeList<?> to) {
        to.offer(an_attribute_instance);
    }
}
```

A block can request instances of a specific attribute by:

1. Finding the appropriate `public static final Attribute<>` field.
    - This defines item attributes in "ItemAttributes", and fluid attributes in "FluidAttributes"
2. Calling either "getAll", "getFirstOrNull", "getFirst", or "get" and passing in the appropriate world, position, and `SearchOption`.

For example a hopper-like block might call this:

```java
/**
 * Finds an {@link ItemInsertable} in the given direction.
 *
 * @param world The world object to search within
 * @param thisPos The position to search from.
 * @param direction The direction to search in.
 * @return An {@link ItemInsertable} that we can attempt to insert into.
 */
public static ItemInsertable getNeighbourInsertable(World world, BlockPos thisPos, Direction direction) {

    BlockPos offsetPos = thisPos.offset(direction);

    // Note that the direction is always the direction of the search:
    // so it's always from the hopper to it's neighbour, and not the
    // side of the neighbour to search.
    SearchOption option = SearchOptions.inDirection(direction);

    // Note that this object will never be null - it will just return
    // a "rejecting" item insertable that never allows anything to be
    // inserted if there's no valid item insertable there. 
    return ItemAttributes.INSERTABLE.get(world, offsetPos, option);
}
```

You can create custom attributes by calling one of the `create` methods in `Attributes`.
Note that attribute instances are never registered anywhere so you should store the
attribute in a `public static final Attribute<MyAttributeClass>` field.

In addition there are three type of attribute:

- Attribute: The base type, implies nothing about attribute instances but only provides the `getAll()` and `getFirstOrNull()` accessor methods.
- DefaultedAttribute: This implies that it is always possible to have a default instance which can used safely everywhere that normal instances could be. This also provides the `getFirst()` accessor method.
- CombinableAttribute: (extends DefaultedAttribute) This implies that it is always possible to combine multiple attribute instances and use them safely everywhere that normal instances can be. This provides the `get()` method which returns a combined version of `getAll()`.

For example a custom attribute based around a fictional class `RobotDockingPort` (which could be used by buildcraft robots) would be defined like this:

```java
public static final Attribute<RobotDockingPort> DOCKING_PORT = Attributes.create(RobotDockingPort.class);
```

Alternatively you can look inside `FluidAttributes` for an example involving combinable attributes.

### Item Management

Everything in the "alexiil.mc.lib.attributes.item" package is dedicated to item management, and is intended for any mod to use.

The core API's are:

- FixedItemInv* (A version of minecraft's Inventory class that only deals with indexed item slot accessors and modification).
- FixedItemInvView* (A read-only version of FixedItemInv).
- GroupedItemInv* (A version of FixedItemInv that operates like a Map<ItemStack, int amount> rather than a List<ItemStack>).
- GroupedItemInvView* (A read-only version of FixedItemInv).
- ItemInsertable* (containing item insertion)
- ItemExtractable* (containing item extraction)
- ItemTransferable (containing item insertion and extraction, which extends ItemInsertable and ItemExtractable).
- ItemFilter (a Predicate for ItemStacks)
- ItemInvSlotChangeListener (for listening to changes in a FixedItemInvView - although not all implementations will support this).

(*Is an attribute in ItemAttributes)

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

- FixedFluidInv*.
- FixedFluidInvView* (A read-only version of FixedFluidInv).
- GroupedFluidInv* (A version of FixedFluidInv that operates like a Map<FluidKey, int amount> rather than a List<FluidVolume>).
- GroupedFluidInvView* (A read-only version of GroupedFluidInv).
- FluidInsertable* (containing fluid insertion)
- FluidExtractable* (containing fluid extraction)
- FluidTransferable (containing fluid insertion and extraction, which extends FluidInsertable and FluidExtractable).
- FluidFilter (a Predicate for FluidKey's)
- FluidInvTankChangeListener (for listening to changes in a FixedItemInvView - although not all implementations will support this).

(*Is an attribute in FluidAttributes)

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
    modCompile "alexiil.mc.lib:libblockattributes:0.3.1"
}
```
