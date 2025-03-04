CREATE TABLE `rental_product_reservation_payment` (
	`uid`	BIGINT	NOT NULL	COMMENT '행 고유키',
	`row_create_date`	DATETIME(3)	NOT NULL	COMMENT '행 생성일',
	`row_update_date`	DATETIME(3)	NOT NULL	COMMENT '행 수정일',
	`row_delete_date_str`	VARCHAR(50)	NOT NULL	DEFAULT /	COMMENT '행 삭제일(yyyy_MM_dd_T_HH_mm_ss_SSS_z, 삭제되지 않았다면 /)',
	`rental_product_reservation_uid`	BIGINT	NOT NULL	COMMENT '상품 예약 정보 행 고유키',
	`payment_uid`	BIGINT	NULL	COMMENT '결제 모듈 테이블 행 고유키'
);

CREATE TABLE `rental_product` (
	`uid`	BIGINT	NOT NULL	COMMENT '행 고유키',
	`row_create_date`	DATETIME(3)	NOT NULL	COMMENT '행 생성일',
	`row_update_date`	DATETIME(3)	NOT NULL	COMMENT '행 수정일',
	`row_delete_date_str`	VARCHAR(50)	NOT NULL	DEFAULT /	COMMENT '행 삭제일(yyyy_MM_dd_T_HH_mm_ss_SSS_z, 삭제되지 않았다면 /)',
	`version_seq`	BIGINT	NOT NULL	COMMENT '예약 상품 정보 버전 시퀀스(고객이 정보를 확인한 시점의 버전과 예약 신청하는 시점의 버전이 다르면 진행 불가)',
	`product_name`	VARCHAR(90)	NOT NULL	COMMENT '상품명',
	`product_intro`	VARCHAR(6000)	NOT NULL	COMMENT '상품 소개',
	`address_country`	VARCHAR(60)	NOT NULL	COMMENT '상품이 위치한 주소(대여 가능 위치의 기준으로 사용됨) - 국가',
	`address_main`	VARCHAR(300)	NOT NULL	COMMENT '상품이 위치한 주소(대여 가능 위치의 기준으로 사용됨) - 국가와 상세 주소를 제외',
	`address_detail`	VARCHAR(300)	NOT NULL	COMMENT '상품이 위치한 주소(대여 가능 위치의 기준으로 사용됨) - 상세',
	`first_reservable_datetime`	DATETIME	NOT NULL	COMMENT '상품 예약이 가능한 최초 일시(콘서트 티켓 예매와 같은 서비스를 가정, 예약 러시 처리가 필요)',
	`first_rental_datetime`	DATETIME	NOT NULL	COMMENT '상품 대여가 가능한 최초 일시',
	`last_rental_datetime`	DATETIME	NULL	COMMENT '상품 대여가 가능한 마지막 일시(null 이라면 제한 없음)',
	`reservation_unit_minute`	BIGINT	NOT NULL	COMMENT '예약 추가 할 수 있는 최소 시간 단위 (분)',
	`minimum_reservation_unit_count`	INT UNSIGNED	NOT NULL	COMMENT '단위 예약 시간을 대여일 기준에서 최소 몇번 추가 해야 하는지',
	`maximum_reservation_unit_count`	INT UNSIGNED	NULL	COMMENT '단위 예약 시간을 대여일 기준에서 최대 몇번 추가 가능한지 (Null 이라면 제한 없음)',
	`reservation_unit_price`	DECIMAL(15, 2)	NOT NULL	COMMENT '단위 예약 시간에 대한 가격 (예약 시간 / 단위 예약 시간 * 예약 단가 = 예약 최종가)',
	`reservation_unit_price_currency_code`	CHAR(3)	NOT NULL	COMMENT '단위 예약 시간에 대한 가격 통화 코드(IOS 4217, ex : KRW, USD, EUR...)',
	`now_reservable`	BIT(1)	NOT NULL	COMMENT '재고, 상품 상태와 상관 없이 현 시점 예약 가능한지에 대한 관리자의 설정',
	`customer_payment_deadline_minute`	BIGINT	NOT NULL	COMMENT '고객에게 이때까지 결제를 해야 한다고 통보하는 기한 설정값(예약일로부터 +N 분)',
	`payment_check_deadline_minute`	BIGINT	NOT NULL	COMMENT '관리자의 결제 확인 기한 설정값(고객 결제 기한 설정값으로 부터 +N 분)',
	`approval_deadline_minute`	BIGINT	NOT NULL	COMMENT '관리자의 예약 승인 기한 설정값(결제 확인 기한 설정값으로부터 +N분)',
	`cancel_deadline_minute`	BIGINT	NOT NULL	COMMENT '고객이 예약 취소 가능한 기한 설정값(대여 시작일로부터 -N분이며, 그 결과가 관리자 승인 기한보다 커야함)',
	`product_desc`	VARCHAR(2000)	NOT NULL	COMMENT '상품 설명(예를 들어 손망실의 경우 now_reservable 이 false 이며, 이곳에 손망실 이유가 기재됩니다.)'
);

CREATE TABLE `rental_product_reservation` (
	`uid`	BIGINT	NOT NULL	COMMENT '행 고유키',
	`row_create_date`	DATETIME(3)	NOT NULL	COMMENT '행 생성일',
	`row_update_date`	DATETIME(3)	NOT NULL	COMMENT '행 수정일',
	`row_delete_date_str`	VARCHAR(50)	NOT NULL	DEFAULT /	COMMENT '행 삭제일(yyyy_MM_dd_T_HH_mm_ss_SSS_z, 삭제되지 않았다면 /)',
	`rental_product_uid`	BIGINT	NULL	COMMENT '예약 상품 정보 행 고유키',
	`customer_member_uid`	BIGINT	NOT NULL	COMMENT '예약자 멤버 행 고유키',
	`rental_start_datetime`	DATETIME	NOT NULL	COMMENT '대여가 시작되는 일시',
	`real_pay_amount`	DECIMAL(15, 2)	NOT NULL	COMMENT '실제 결제 해야 하는 금액(할인 등을 적용한 이후, 통화 코드는 예약 정보의 가격 정보와 동일)',
	`rental_end_datetime`	DATETIME	NOT NULL	COMMENT '대여가 끝나는 일시 (회수 시간은 포함되지 않는 순수 서비스 이용 시간)',
	`customer_payment_deadline_datetime`	DATETIME	NOT NULL	COMMENT '고객에게 이때까지 결제를 해야 한다고 통보한 기한',
	`payment_check_deadline_datetime`	DATETIME	NOT NULL	COMMENT '예약 결재 확인 기한 (결재 기한 초과 처리.)',
	`approval_deadline_datetime`	DATETIME	NOT NULL	COMMENT '관리자 승인 기한 (이 시점이 지났고, reservation_approval_datetime 가 충족되지 않았다면 취소로 간주)',
	`cancel_deadline_datetime`	DATETIME	NOT NULL	COMMENT '예약 취소 가능 기한',
	`product_ready_datetime`	DATETIME	NULL	COMMENT '상품이 대여 반납 이후 준비가 완료된 시간(미리 설정도 가능, 히스토리 테이블 내역보다 우선됩니다.)',
	`version_seq`	BIGINT	NOT NULL	COMMENT '예약 상품 정보 버전 시퀀스(아래 부터는 예약 당시의 정보로, 영수증의 기능을 위한 정보 복제 컬럼)',
	`product_name`	VARCHAR(90)	NOT NULL	COMMENT '고객에게 보일 상품명',
	`product_intro`	VARCHAR(6000)	NOT NULL	COMMENT '고객에게 보일 상품 소개',
	`address_country`	VARCHAR(60)	NOT NULL	COMMENT '상품이 위치한 주소(대여 가능 위치의 기준으로 사용됨) - 국가',
	`address_main`	VARCHAR(300)	NOT NULL	COMMENT '상품이 위치한 주소(대여 가능 위치의 기준으로 사용됨) - 국가와 상세 주소를 제외',
	`address_detail`	VARCHAR(300)	NOT NULL	COMMENT '상품이 위치한 주소(대여 가능 위치의 기준으로 사용됨) - 상세',
	`reservation_unit_minute`	BIGINT	NOT NULL	COMMENT '예약 추가 할 수 있는 최소 시간 단위 (분)',
	`reservation_unit_price`	DECIMAL(15, 2)	NOT NULL	COMMENT '단위 예약 시간에 대한 가격 (예약 시간 / 단위 예약 시간 * 예약 단가 = 예약 최종가)',
	`reservation_unit_price_currency_code`	CHAR(3)	NOT NULL	COMMENT '단위 예약 시간에 대한 가격 통화 코드(IOS 4217, ex : KRW, USD, EUR...)'
);

CREATE TABLE `rental_product_reservation_image` (
	`uid`	BIGINT	NOT NULL	COMMENT '행 고유키',
	`row_create_date`	DATETIME(3)	NOT NULL	COMMENT '행 생성일',
	`row_update_date`	DATETIME(3)	NOT NULL	COMMENT '행 수정일',
	`row_delete_date_str`	VARCHAR(50)	NOT NULL	DEFAULT /	COMMENT '행 삭제일(yyyy_MM_dd_T_HH_mm_ss_SSS_z, 삭제되지 않았다면 /)',
	`rental_product_reservation_uid`	BIGINT	NOT NULL	COMMENT '상품 예약 정보 행 고유키',
	`image_full_url`	VARCHAR(200)	NOT NULL	COMMENT '프로필 이미지 Full URL',
	`priority`	MEDIUMINT UNSIGNED	NOT NULL	COMMENT '가중치(높을수록 전면에 표시되며, 동일 가중치의 경우 최신 정보가 우선됩니다.)'
);

CREATE TABLE `rental_product_reservation_history` (
	`uid`	BIGINT	NOT NULL	COMMENT '행 고유키',
	`row_create_date`	DATETIME(3)	NOT NULL	COMMENT '행 생성일',
	`row_update_date`	DATETIME(3)	NOT NULL	COMMENT '행 수정일',
	`row_delete_date_str`	VARCHAR(50)	NOT NULL	DEFAULT /	COMMENT '행 삭제일(yyyy_MM_dd_T_HH_mm_ss_SSS_z, 삭제되지 않았다면 /)',
	`rental_product_reservation_uid`	BIGINT	NOT NULL	COMMENT '상품 예약 정보 행 고유키',
	`history_code`	TINYINT UNSIGNED	NOT NULL	COMMENT '히스토리 코드(0 : 예약 신청, 1 : 예약 신청 거부, 2 : 예약 신청 승인, 3 : 예약 신청 승인 취소, 4 : 예약 취소 신청, 5 : 예약 취소 신청 철회, 6 : 예약 취소 신청 승인, 7 : 예약 취소 신청 거부, 8 :결제 확인, 9 : 결제 확인 취소, 10 : 상품 조기 반납 신고, 11 : 상품 조기 반납 취소, 12 : 상품 반납 확인, 13 : 상품 연체 상태, 14 : 상품 연체 상태 취소, 15 : 예약 시간 연장 신청, 16 : 예약 시간 연장 신청 철회, 17 : 예약 시간 연장 신청 거부, 18 : 예약 시간 연장 신청 승인)',
	`history_desc`	VARCHAR(600)	NOT NULL	COMMENT '히스토리 상세(예약 연장 신청 히스토리라면 yyyy_MM_dd_T_HH_mm_ss_z 형태의 연장 시간으로 시작해서 / 를 입력 후 뒤에 추가 설명이 붙습니다.)'
);

CREATE TABLE `rental_product_image` (
	`uid`	BIGINT	NOT NULL	COMMENT '행 고유키',
	`row_create_date`	DATETIME(3)	NOT NULL	COMMENT '행 생성일',
	`row_update_date`	DATETIME(3)	NOT NULL	COMMENT '행 수정일',
	`row_delete_date_str`	VARCHAR(50)	NOT NULL	DEFAULT /	COMMENT '행 삭제일(yyyy_MM_dd_T_HH_mm_ss_SSS_z, 삭제되지 않았다면 /)',
	`rental_product_uid`	BIGINT	NOT NULL	COMMENT '예약 상품 정보 행 고유키',
	`image_full_url`	VARCHAR(200)	NOT NULL	COMMENT '프로필 이미지 Full URL',
	`priority`	MEDIUMINT UNSIGNED	NOT NULL	COMMENT '가중치(높을수록 전면에 표시되며, 동일 가중치의 경우 최신 정보가 우선됩니다.)'
);

ALTER TABLE `rental_product_reservation_payment` ADD CONSTRAINT `PK_RENTAL_PRODUCT_RESERVATION_PAYMENT` PRIMARY KEY (
	`uid`
);

ALTER TABLE `rental_product` ADD CONSTRAINT `PK_RENTAL_PRODUCT` PRIMARY KEY (
	`uid`
);

ALTER TABLE `rental_product_reservation` ADD CONSTRAINT `PK_RENTAL_PRODUCT_RESERVATION` PRIMARY KEY (
	`uid`
);

ALTER TABLE `rental_product_reservation_image` ADD CONSTRAINT `PK_RENTAL_PRODUCT_RESERVATION_IMAGE` PRIMARY KEY (
	`uid`
);

ALTER TABLE `rental_product_reservation_history` ADD CONSTRAINT `PK_RENTAL_PRODUCT_RESERVATION_HISTORY` PRIMARY KEY (
	`uid`
);

ALTER TABLE `rental_product_image` ADD CONSTRAINT `PK_RENTAL_PRODUCT_IMAGE` PRIMARY KEY (
	`uid`
);

