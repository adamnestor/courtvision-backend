package com.adamnestor.courtvision.domain;

import com.adamnestor.courtvision.domain.StatCategory;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_picks")
public class UserPicks {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private Players player;

    @ManyToOne
    @JoinColumn(name = "game_id", nullable = false)
    private Games game;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatCategory category;

    @Column(nullable = false)
    private Integer threshold;

    @Column(name = "hit_rate_at_pick")
    private BigDecimal hitRateAtPick;

    private Boolean result;