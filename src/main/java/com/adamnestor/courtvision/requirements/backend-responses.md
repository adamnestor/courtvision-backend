# Backend Response Requirements

## 1. Player Stats Endpoint (GET /players/{id}/stats)

```json
{
  "data": {
    "playerId": 123,
    "playerName": "LeBron James",
    "team": "LAL",
    "hitRate": 75.5,
    "confidenceScore": 85,
    "gamesPlayed": 10,
    "average": 28.5,
    "isHighConfidence": true
  }
}
```

## 2. Picks Endpoint (GET /picks)

```json
{
  "data": [
    {
      "id": 1,
      "playerId": 123,
      "playerName": "LeBron James",
      "team": "LAL",
      "opponent": "GSW",
      "category": "POINTS",
      "threshold": 25,
      "hitRateAtPick": 75.5,
      "confidenceScore": 85,
      "result": "WIN",
      "createdAt": "2024-01-19T12:00:00Z",
      "gameTime": "2024-01-19T20:00:00Z"
    }
  ]
}
```

## 3. Dashboard Stats Endpoint

GET /dashboard/stats

- page (default: 0)
- size (default: 20)
- timeframe (optional: "7D", "30D", "SEASON")
- category (optional: "POINTS", "REBOUNDS", "ASSISTS")
- threshold (optional: integer)

Example: `/dashboard/stats?page=0&size=20&timeframe=7D&category=POINTS&threshold=25`

```json
{
  "data": [
    {
      "playerId": 123,
      "playerName": "LeBron James",
      "team": "LAL",
      "opponent": "PHX",
      "isAway": true,
      "gameTime": "7:30 PM",
      "category": "POINTS",
      "hitRate": 75.5,
      "confidenceScore": 85,
      "gamesPlayed": 10,
      "average": 28.5,
      "lastGames": [25, 30, 22],
      "isHighConfidence": true
    }
  ],
  "meta": {
    "totalGames": 8,
    "appliedFilters": {
      "timeframe": "7D",
      "category": "POINTS",
      "threshold": 25
    }
  },
  "pagination": {
    "currentPage": 0,
    "pageSize": 20,
    "totalPages": 47,
    "totalElements": 924
  }
}
```

## Confidence Score Ranges

- 80-100: High confidence (green)
- 60-79: Medium confidence (yellow)
- 0-59: Low confidence (muted)

Notes:

- All confidence scores should be integers between 0-100
- Result can be "WIN", "LOSS", or null for pending picks
- Dates should be in ISO 8601 format

## Frontend Value Expectations

### Hit Rate

- Should be a number with one decimal place (e.g., 75.5)
- Valid range: 0.0 to 100.0
- Displayed with % symbol (e.g., "75.5%")
- Values >= 70.0 show a green checkmark indicator

### Confidence Score

- Must be an integer (no decimals)
- Valid range: 0 to 100
- Display colors:
  - 80-100: Success/Green (high confidence)
  - 60-79: Warning/Yellow (medium confidence)
  - 0-59: Muted/Gray (low confidence)
- Always shown with info tooltip explaining the score
