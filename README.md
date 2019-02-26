# LibBlockAttributes

This is a library mod for the [Fabric](https://fabricmc.net/) API, based around [Minecraft](https://minecraft.net).

This project has 2 major concepts: attributes, and item management.

### Attributes

Unfortunately this part of the API hasn't been very thoroughly thought out :(

An "attribute" is just a class that has an instance of "alexiil.mc.lib.attributes.Attribute" stored in a public static final field.

Currently there are 2 ways that a block can provide attribute instances: either implement IAttributeBlock and return the attribute instances directly or implement IDelegatingAttributeBlock and return IAttributeProvider instances that return the attributes instead.

### Item Management

Everything in the "alexiil.mc.lib.attributes.item" package is dedicated to item management, and is intended for any mod to use.

The core API's are:

- IFixedItemInv* (A version of minecraft's Inventory class that only deals with indexed item slot accessors and modification).
- IFixedItemInvView* (A read-only version of IFixedItemInv).
- IItemInvStats* (containing statistics for any inventory).
- IItemInsertable* (containing item insertion)
- IItemExtractable* (containing item extraction)
- IItemFilter (a Predicate for ItemStacks)
- IInvSlotChangeListener (for listening to changes in an IFixedItemInvView - although not all implementations will support this).

(*Is an attribute by default)

In addition there are various utility classes:

- ItemInvUtil (for accessing attribute versions of the above API's, and a "move" method for moving items from an IItemExtractable to an IItemInsertable).
- ItemStackUtil (containing general methods around single ItemStack instances that were needed to create this API).
- ItemStackCollections (utility methods for creating Sets and Maps that compare ItemStack's correctly (as ItemStack itself doesn't override equals() or hashCode()).

The "impl" subpackage is also for public use, and it contains a lot of concrete implementations for the core API's.
