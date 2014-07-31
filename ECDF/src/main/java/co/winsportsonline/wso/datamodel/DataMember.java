package co.winsportsonline.wso.datamodel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DataMember {
    String member ();
}
