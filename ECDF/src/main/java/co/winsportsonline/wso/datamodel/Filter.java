package co.winsportsonline.wso.datamodel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Franklin Cruz on 27-02-14.
 */
public class Filter implements DataModel {

    private int order;
    private String name;
    private List<String> categories = new ArrayList<String>();
    private List<Filter> filters = new ArrayList<Filter>();
    private String image;

    private Filter parent;

    @DataMember(member = "items")
    public List<Filter> getFilters() {
        return filters;
    }

    @DataMember(member = "items")
    public void setFilters(List<Filter> filters) {
        this.filters = filters;
    }

    @DataMember(member = "order")
    public int getOrder() {
        return order;
    }

    @DataMember(member = "order")
    public void setOrder(int order) {
        this.order = order;
    }

    @DataMember(member = "name")
    public String getName() {
        return name;
    }

    @DataMember(member = "name")
    public void setName(String name) {
        this.name = name;
    }

    @DataMember(member = "categories")
    public List<String> getCategories() {
        return categories;
    }

    @DataMember(member = "categories")
    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    @DataMember(member = "image")
    public String getImage() {
        return image;
    }

    @DataMember(member = "image")
    public void setImage(String image) {
        this.image = image;
    }

    public Filter getParent() {
        return parent;
    }

    public void setParent(Filter parent) {
        this.parent = parent;
    }
}
