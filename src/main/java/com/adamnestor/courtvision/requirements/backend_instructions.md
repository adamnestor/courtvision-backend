# NBA Stats Analysis Tool Documentation

## 1. Project Overview
A comprehensive NBA statistics analysis tool that calculates and displays player performance hit rates and confidence scores across various statistical categories. The system analyzes historical performance, game context, and various other factors to provide data-driven insights.

### Core Capabilities
- Track player performance across multiple statistical categories
- Calculate hit rates for different thresholds and time periods
- Generate confidence scores using advanced analytics
- Display detailed player performance visualizations
- Monitor and evaluate prediction accuracy

## 2. Feature Requirements

### 2.1 Dashboard Features
- **Hit Rate Overview**
  - Display player hit rates for various statistical thresholds
  - Support multiple time period views
  - Enable dynamic filtering
  - Sort by hit rates or confidence scores

- **Statistical Categories & Thresholds**
  - Points: 10+, 15+, 20+, 25+
  - Assists: 2+, 4+, 6+, 8+
  - Rebounds: 4+, 6+, 8+, 10+

- **Time Period Options**
  - Last 5 games (L5)
  - Last 10 games (L10) - DEFAULT VIEW
  - Last 15 games (L15)
  - Last 20 games (L20)
  - Season total

### 2.2 Player Detail View
- Interactive bar graph visualization
- Game-by-game performance display
- Color-coded performance indicators
- Comprehensive filtering capabilities
- Display of matchup-specific insights

### 2.3 Confidence Score System

The confidence score is calculated using multiple weighted components and adjustments:

#### Base Components (Initial Calculation)
1. Recent Performance (35%)
   - Uses exponential decay weighting for last 10 games
   - Includes "near miss" calculations for close performances
   - Decay factor: 0.15

2. Advanced Metrics Impact (30%)
   - Player Impact Estimate (PIE)
   - Usage Rate Analysis
   - Category-specific efficiency metrics:
     - Points: True Shooting %
     - Assists: Assist %
     - Rebounds: Rebound %

3. Game Context (35%)
   - Matchup history analysis
   - Team defensive metrics
   - Pace impact calculations
   - Venue impact assessment

#### Additional Adjustments

4. Rest Impact
   - Back-to-back games: ×0.90 (-10%)
   - One day rest: No adjustment
   - Two days rest: ×1.02 (+2%)
   - Three+ days rest: ×1.05 (+5%)
   - Includes historical performance analysis by rest days

5. Blowout Risk Adjustment
   - Only applied when risk exceeds 60%
   - Category-specific impact:
     - Points: 100% of calculated reduction
     - Assists: 80% of calculated reduction
     - Rebounds: 60% of calculated reduction
   - Maximum confidence reduction: 50%
   - Uses team strength differentials and historical matchup patterns

### 2.4 Evaluation System
- Data Collection Framework
- Accuracy Analysis Process
- Component Analysis
- Review & Adjustment Cycle
- Pattern Recognition System

## Current File Structure
src/
├── main/
│   └── java/
│       └── com/
│           └── adamnestor/
│               └── courtvision/
│                   ├── confidence/
│                   │   ├── model/
│                   │   │   ├── AdvancedImpact.java
│                   │   │   ├── BlowoutImpact.java
│                   │   │   ├── GameContext.java
│                   │   │   └── RestImpact.java
│                   │   ├── service/
│                   │   │   ├── impl/
│                   │   │   │   ├── AdvancedMetricsServiceImpl.java
│                   │   │   │   ├── BlowoutRiskServiceImpl.java
│                   │   │   │   ├── ConfidenceScoreServiceImpl.java
│                   │   │   │   ├── GameContextServiceImpl.java
│                   │   │   │   └── RestImpactServiceImpl.java
│                   │   │   ├── AdvancedMetricsService.java
│                   │   │   ├── BlowoutRiskService.java
│                   │   │   ├── ConfidenceScoreService.java
│                   │   │   ├── GameContextService.java
│                   │   │   └── RestImpactService.java
│                   │   └── util/
│                   │       ├── BlowoutCalculator.java
│                   │       ├── ContextCalculator.java
│                   │       └── RestCalculator.java
│                   ├── service/
│                   │   ├── impl/
│                   │   │   └── HitRateCalculationServiceImpl.java
│                   │   ├── util/
│                   │   │   ├── DateUtils.java
│                   │   │   └── StatAnalysisUtils.java
│                   │   └── HitRateCalculationService.java
│                   ├── mapper/
│                   │   ├── DashboardMapper.java
│                   │   └── PlayerMapper.java
│                   └── web/
│                       ├── PlayerController.java
│                       └── UserPickController.java
└── test/
    └── java/
        └── com/
            └── adamnestor/
                └── courtvision/
                    └── test/
                        └── confidence/
                            └── service/
                                └── impl/
                                    ├── AdvancedMetricsServiceImplTest.java
                                    ├── ConfidenceScoreServiceImplTest.java
                                    └── GameContextServiceImplTest.java

## 4. Implementation Notes

### 4.1 Data Sources
- Primary API: BallDontLie API
- Data refresh schedule: Daily at 4am ET
- Backup data collection window: 4:30-5:00am ET

### 4.2 Caching Strategy
- Redis implementation
- Cache warming: 5:00-5:30am ET
- LRU eviction policy
- Monitored metrics:
  - Cache hit/miss rates
  - Memory usage
  - Performance under load

### 4.3 Error Handling
- Comprehensive logging
- Admin notifications for failures
- Fallback to previous day's data
- Automated retry schedule