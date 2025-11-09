-- 13-ad-timing-policy.sql
-- 광고 노출 시간/간격 정책 추가
-- 작성일: 2025-11-09

ALTER TABLE public.ad_policy
ADD COLUMN IF NOT EXISTS ad_allowed_start_hour INT DEFAULT 0 NOT NULL,
ADD COLUMN IF NOT EXISTS ad_allowed_end_hour INT DEFAULT 23 NOT NULL,
ADD COLUMN IF NOT EXISTS ad_app_open_delay_seconds INT DEFAULT 5 NOT NULL,
ADD COLUMN IF NOT EXISTS ad_interstitial_min_interval_seconds INT DEFAULT 120 NOT NULL,
ADD COLUMN IF NOT EXISTS ad_suppression_until TIMESTAMP WITH TIME ZONE DEFAULT NULL;

-- 릴리즈 기본값 안전 설정 (글로벌 출시: 24시간 허용)
UPDATE public.ad_policy
SET ad_allowed_start_hour = 0,
    ad_allowed_end_hour = 23,
    ad_app_open_delay_seconds = 5,
    ad_interstitial_min_interval_seconds = 120,
    ad_suppression_until = NULL
WHERE app_id = 'com.sweetapps.pocketchord';

-- 디버그 기본값(테스트용)
UPDATE public.ad_policy
SET ad_allowed_start_hour = 0,
    ad_allowed_end_hour = 23,
    ad_app_open_delay_seconds = 2,
    ad_interstitial_min_interval_seconds = 30,
    ad_suppression_until = NULL
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 확인
SELECT app_id, ad_allowed_start_hour, ad_allowed_end_hour, ad_app_open_delay_seconds, ad_interstitial_min_interval_seconds, ad_suppression_until
FROM public.ad_policy
WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
