package entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import java.sql.Date;
import java.util.List;

@Getter
@Setter
@ToString(exclude = "pilotResults")
@Builder
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "versions")
public class Version {
    @Id
    @Column(name = "version_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int VersionId;

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "id.version", orphanRemoval = true)
    private List<FishnetData> fishnetDataList;

    @Column(name = "description")
    private String desc;
    @Column(name = "creation_date")
    private Date date;

}
