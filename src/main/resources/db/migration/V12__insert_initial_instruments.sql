-- V12: Insert initial instrument data for major KOSPI/KOSDAQ stocks
-- This provides seed data for MVP testing

INSERT INTO instruments (
    symbol, market, name_kr, name_en, sector_code, industry,
    tick_size, lot_size, listing_date, status, tradable, halted,
    created_at, updated_at
) VALUES
    -- KOSPI Blue Chips
    ('005930', 'KOSPI', '삼성전자', 'Samsung Electronics', 'G2520', '반도체',
     5.00, 1, '1975-06-11', 'LISTED', TRUE, FALSE, NOW(3), NOW(3)),

    ('035420', 'KOSPI', 'NAVER', 'NAVER Corp', 'G4540', '인터넷서비스',
     50.00, 1, '2002-10-29', 'LISTED', TRUE, FALSE, NOW(3), NOW(3)),

    ('000660', 'KOSPI', 'SK하이닉스', 'SK Hynix', 'G2520', '반도체',
     100.00, 1, '1996-12-26', 'LISTED', TRUE, FALSE, NOW(3), NOW(3)),

    ('051910', 'KOSPI', 'LG화학', 'LG Chem', 'G2010', '화학',
     500.00, 1, '2001-04-26', 'LISTED', TRUE, FALSE, NOW(3), NOW(3)),

    ('005380', 'KOSPI', '현대차', 'Hyundai Motor', 'G3010', '자동차',
     100.00, 1, '1974-10-02', 'LISTED', TRUE, FALSE, NOW(3), NOW(3)),

    ('005490', 'KOSPI', 'POSCO홀딩스', 'POSCO Holdings', 'G1510', '철강',
     500.00, 1, '1988-10-10', 'LISTED', TRUE, FALSE, NOW(3), NOW(3)),

    ('028260', 'KOSPI', '삼성물산', 'Samsung C&T', 'G2030', '건설',
     100.00, 1, '1997-07-01', 'LISTED', TRUE, FALSE, NOW(3), NOW(3)),

    ('012330', 'KOSPI', '현대모비스', 'Hyundai Mobis', 'G3030', '자동차부품',
     100.00, 1, '2000-03-23', 'LISTED', TRUE, FALSE, NOW(3), NOW(3)),

    ('066570', 'KOSPI', 'LG전자', 'LG Electronics', 'G2550', '가전',
     100.00, 1, '2002-08-27', 'LISTED', TRUE, FALSE, NOW(3), NOW(3)),

    ('207940', 'KOSPI', '삼성바이오로직스', 'Samsung Biologics', 'G2130', '바이오',
     1000.00, 1, '2016-11-10', 'LISTED', TRUE, FALSE, NOW(3), NOW(3)),

    -- KOSDAQ
    ('247540', 'KOSDAQ', '에코프로비엠', 'EcoPro BM', 'G2010', '2차전지소재',
     100.00, 1, '2016-11-02', 'LISTED', TRUE, FALSE, NOW(3), NOW(3)),

    ('086520', 'KOSDAQ', '에코프로', 'EcoPro', 'G2010', '환경',
     100.00, 1, '2007-05-25', 'LISTED', TRUE, FALSE, NOW(3), NOW(3)),

    ('096530', 'KOSDAQ', '씨젠', 'Seegene', 'G2130', '진단',
     100.00, 1, '2010-07-23', 'LISTED', TRUE, FALSE, NOW(3), NOW(3)),

    ('068270', 'KOSDAQ', '셀트리온', 'Celltrion', 'G2130', '바이오의약품',
     100.00, 1, '2008-07-01', 'LISTED', TRUE, FALSE, NOW(3), NOW(3)),

    ('091990', 'KOSDAQ', '셀트리온헬스케어', 'Celltrion Healthcare', 'G2130', '바이오',
     50.00, 1, '2017-07-20', 'LISTED', TRUE, FALSE, NOW(3), NOW(3)),

    ('293490', 'KOSDAQ', '카카오게임즈', 'Kakao Games', 'G4540', '게임',
     50.00, 1, '2020-09-10', 'LISTED', TRUE, FALSE, NOW(3), NOW(3)),

    ('263750', 'KOSDAQ', '펄어비스', 'Pearl Abyss', 'G4540', '게임',
     100.00, 1, '2017-09-14', 'LISTED', TRUE, FALSE, NOW(3), NOW(3)),

    ('357780', 'KOSDAQ', '솔브레인', 'Soulbrain', 'G2520', '전자재료',
     100.00, 1, '2020-07-02', 'LISTED', TRUE, FALSE, NOW(3), NOW(3));

-- Verify insertion
SELECT
    market,
    COUNT(*) as count,
    GROUP_CONCAT(name_kr ORDER BY symbol SEPARATOR ', ') as samples
FROM instruments
GROUP BY market;
