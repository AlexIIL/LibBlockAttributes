package alexiil.mc.lib.attributes;

public interface IAttributeProvider {

    <T> void addAllAttributes(AttributeList<T> to);
}
