package com.adamnestor.courtvision.confidence.service;

import com.adamnestor.courtvision.confidence.model.GameContext;
import com.adamnestor.courtvision.domain.*;

public interface GameContextService {
    GameContext calculateGameContext(Players player, Games game, StatCategory category);
}