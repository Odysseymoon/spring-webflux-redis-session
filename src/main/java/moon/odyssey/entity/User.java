package moon.odyssey.entity;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "user")
@Data
@ToString
@EqualsAndHashCode(of = {"userId"})
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    @Id
    private String userId;

    private String password;
}
