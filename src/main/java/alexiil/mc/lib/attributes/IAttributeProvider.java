package alexiil.mc.lib.attributes;

public interface IAttributeProvider {
    <T> T getAttribute(Attribute<T> attribute);
}
