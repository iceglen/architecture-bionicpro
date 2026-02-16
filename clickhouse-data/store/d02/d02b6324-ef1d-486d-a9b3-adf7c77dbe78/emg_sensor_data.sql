ATTACH TABLE _ UUID 'f50dbd47-1648-4507-b8cc-2d383ff7c459'
(
    `user_id` UInt32,
    `prosthesis_type` String,
    `muscle_group` String,
    `signal_frequency` UInt32,
    `signal_duration` UInt32,
    `signal_amplitude` Decimal(5, 2),
    `signal_time` DateTime
)
ENGINE = MergeTree
ORDER BY (user_id, prosthesis_type, signal_time)
SETTINGS index_granularity = 8192
