package entity;

import lombok.*;
import org.hibernate.Hibernate;

import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

@Embeddable
public class FishnetVersionKey implements Serializable {
    private static final long serialVersionUID = -3087213028620269731L;

    @ManyToOne
    @JoinColumn(name = "cell_id", nullable = false)
    private FishnetStatic fishnetStatic;

    @ManyToOne
    @JoinColumn(name = "version_id", nullable = false)
    private Version version;

    @Override
    public String toString() {
        return "FishnetVersionKey{" +
                "fishnetId=" + fishnetStatic.getId() +
                ", versionId=" + version.getVersionId() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        FishnetVersionKey that = (FishnetVersionKey) o;
        return (fishnetStatic != null && version != null) && Objects.equals(fishnetStatic, that.fishnetStatic)
                && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
