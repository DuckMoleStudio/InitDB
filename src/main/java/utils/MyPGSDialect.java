package utils;

import com.vladmihalcea.hibernate.type.array.DoubleArrayType;
import com.vladmihalcea.hibernate.type.array.StringArrayType;
import org.hibernate.spatial.dialect.postgis.PostgisPG92Dialect;

public class MyPGSDialect extends PostgisPG92Dialect {
    public MyPGSDialect()
    {
        super();
        this.registerHibernateType(2003,StringArrayType.class.getName());

    }
}
