package test.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.ixtf.persistence.IEntity;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

import java.io.Serializable;
import java.util.Collection;

@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@Getter
@Setter
@Entity
public class Operator implements IEntity {
    @ToString.Include
    @EqualsAndHashCode.Include
    @Id
    private String id;
    @JsonIgnore
    private boolean deleted;
    @ToString.Include
    private String name;
    @JsonIgnore
    private Collection<String> pyIdx;

    public OperatorEmbeddable embeddable() {
        final var ret = new OperatorEmbeddable();
        ret.setId(id);
        ret.setName(name);
        return ret;
    }

    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @NoArgsConstructor
    @Data
    @Embeddable
    public static class OperatorEmbeddable implements Serializable {
        @EqualsAndHashCode.Include
        private String id;
        private String name;
    }
}
