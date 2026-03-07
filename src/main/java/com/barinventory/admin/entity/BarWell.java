package com.barinventory.admin.entity;




import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bar_wells")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class BarWell {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 @ManyToOne(fetch = FetchType.LAZY)
 @JoinColumn(name = "bar_id", nullable = false)
 private Bar bar;

 @Column(nullable = false, length = 50)
 private String wellName; // BAR_1, BAR_2, SERVICE_BAR

 @Column(nullable = false)
 private boolean active = true;
}