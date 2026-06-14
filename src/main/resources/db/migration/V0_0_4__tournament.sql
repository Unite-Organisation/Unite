CREATE TABLE tournament
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    building_id UUID REFERENCES building(id) ON DELETE CASCADE NOT NULL,
    created_by UUID REFERENCES app_user(id) ON DELETE CASCADE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    team_size INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    type VARCHAR(255),
    status VARCHAR(255) NOT NULL DEFAULT 'OPEN'
);

CREATE TABLE tournament_team
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id UUID REFERENCES tournament(id) ON DELETE CASCADE NOT NULL,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE team_member
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID REFERENCES tournament_team(id) ON DELETE CASCADE NOT NULL,
    user_id UUID REFERENCES app_user(id) ON DELETE CASCADE NOT NULL,
    UNIQUE (team_id, user_id)
);

CREATE TABLE tournament_match
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id UUID REFERENCES tournament(id) ON DELETE CASCADE NOT NULL,
    next_match_id UUID REFERENCES tournament_match(id) ON DELETE CASCADE,
    round_number INT NOT NULL,
    team_a_id UUID REFERENCES tournament_team(id) ON DELETE SET NULL,
    team_b_id UUID REFERENCES tournament_team(id) ON DELETE SET NULL,
    winner_team_id UUID REFERENCES tournament_team(id) ON DELETE SET NULL,
    is_skip BOOLEAN NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING' NOT NULL
);

CREATE TABLE tournament_participant
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id UUID REFERENCES tournament(id) ON DELETE CASCADE NOT NULL,
    user_id UUID REFERENCES app_user(id) ON DELETE CASCADE NOT NULL,
    UNIQUE (tournament_id, user_id)
);