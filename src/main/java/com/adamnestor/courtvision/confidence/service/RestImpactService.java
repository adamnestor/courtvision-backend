package com.adamnestor.courtvision.confidence.service;

import com.adamnestor.courtvision.confidence.model.RestImpact;
import com.adamnestor.courtvision.domain.*;

public interface RestImpactService {
    RestImpact calculateRestImpact(Players player, Games currentGame);
}