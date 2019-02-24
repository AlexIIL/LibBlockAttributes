package alexiil.mc.lib.attributes;

public enum AttributeProviderSearchState implements IAttributeProvider {
    END_OF_PHYSICAL_ACCESS;

    @Override
    public <T> T getAttribute(Attribute<T> attribute) {
        return null;
    }
}
