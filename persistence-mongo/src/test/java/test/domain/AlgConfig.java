package test.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mongodb.DBRef;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import test.domain.Operator.OperatorEmbeddable;

import java.util.Collection;
import java.util.Date;

@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@Getter
@Setter
@Entity
public class AlgConfig implements Comparable<AlgConfig> {
    @ToString.Include
    @EqualsAndHashCode.Include
    @Id
    @NotBlank
    private String id;
    @JsonIgnore
    private boolean deleted;
    @JsonIgnore
    @NotNull
    private OperatorEmbeddable creator;
    @JsonIgnore
    @NotNull
    private Date createDateTime;
    @JsonIgnore
    private OperatorEmbeddable modifier;
    @JsonIgnore
    private Date modifyDateTime;
    private String note;
    @NotBlank
    private String version;
    @Min(0)
    @Max(1)
    private double confidenceThreshold;
    @Min(0)
    @Max(1)
    private double consistencyThreshold;
    private Collection<DBRef> auxiliaryShapeLabels;
    private Collection<DBRef> dependencies;
    // 是否初始，版本刚刚升级，或新的算法，需要红点标识
    private boolean init;
    private String filterMvel;

    @Override
    public int compareTo(AlgConfig o) {
        return id.compareTo(o.id);
    }


}
