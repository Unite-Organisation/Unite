--
-- PostgreSQL database dump
--

\restrict bqpdEOUscPBTiiV0rF1iSPnYygLPxWWP8HIbfZRVPDbY2pYBkbtKXU5QkYUhuPw

-- Dumped from database version 15.14 (Debian 15.14-1.pgdg13+1)
-- Dumped by pg_dump version 15.14 (Debian 15.14-1.pgdg13+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: enforce_user_building_id_rule(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.enforce_user_building_id_rule() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    role_name TEXT;
BEGIN
    SELECT user_role INTO role_name
    FROM user_role
    WHERE id = NEW.user_role;

    IF role_name IN ('ADMIN', 'MANAGER') AND NEW.building_id IS NOT NULL THEN
        RAISE EXCEPTION 'Users with role % cannot have building_id set', role_name;
    END IF;

    RETURN NEW;
END;
$$;


ALTER FUNCTION public.enforce_user_building_id_rule() OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: app_user; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.app_user (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    first_name character varying(20) NOT NULL,
    last_name character varying(20) NOT NULL,
    email character varying(50),
    username character varying(20) NOT NULL,
    password character varying(200) NOT NULL,
    user_role uuid,
    status character varying(30) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    building_id uuid
);


ALTER TABLE public.app_user OWNER TO postgres;

--
-- Name: area; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.area (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(30) NOT NULL,
    country character varying(30) NOT NULL,
    city character varying(30) NOT NULL,
    type character varying(30) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.area OWNER TO postgres;

--
-- Name: building; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.building (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(30),
    country character varying(30),
    city character varying(30),
    street character varying(50),
    number character varying(10),
    area_id uuid NOT NULL
);


ALTER TABLE public.building OWNER TO postgres;

--
-- Name: building_manager; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.building_manager (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    building_id uuid NOT NULL,
    user_id uuid NOT NULL
);


ALTER TABLE public.building_manager OWNER TO postgres;

--
-- Name: conversation; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.conversation (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    is_group boolean NOT NULL,
    name character varying(50),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.conversation OWNER TO postgres;

--
-- Name: conversation_member; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.conversation_member (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid,
    conversation_id uuid,
    join_date timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.conversation_member OWNER TO postgres;

--
-- Name: facility; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.facility (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(50) NOT NULL,
    building_id uuid,
    type character varying(50),
    capacity integer,
    location character varying(255),
    requires_approval boolean DEFAULT false
);


ALTER TABLE public.facility OWNER TO postgres;

--
-- Name: facility_reservation; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.facility_reservation (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    facility_id uuid NOT NULL,
    user_id uuid NOT NULL,
    start_time timestamp without time zone NOT NULL,
    end_time timestamp without time zone NOT NULL,
    status character varying(50) NOT NULL,
    purpose character varying(255),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.facility_reservation OWNER TO postgres;

--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.flyway_schema_history OWNER TO postgres;

--
-- Name: issue; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.issue (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    title character varying(256) NOT NULL,
    description text NOT NULL,
    status character varying(30),
    priority character varying(30) NOT NULL,
    area_id uuid,
    building_id uuid,
    facility_id uuid,
    poll_id uuid,
    notify_everyone boolean NOT NULL,
    created_by uuid NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    issue_object character varying(50) DEFAULT 'AREA'::character varying NOT NULL
);


ALTER TABLE public.issue OWNER TO postgres;

--
-- Name: message; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.message (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    sender_id uuid,
    conversation_id uuid,
    content text,
    send_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.message OWNER TO postgres;

--
-- Name: messages_read; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.messages_read (
    message_id uuid NOT NULL,
    viewed_by uuid NOT NULL,
    viewed_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.messages_read OWNER TO postgres;

--
-- Name: notification; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.notification (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    issue_id uuid,
    recipient_id uuid NOT NULL,
    seen_at timestamp without time zone
);


ALTER TABLE public.notification OWNER TO postgres;

--
-- Name: offering; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.offering (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    title character varying(50) NOT NULL,
    description text NOT NULL,
    category character varying(50) NOT NULL,
    is_active boolean DEFAULT true,
    area_id uuid NOT NULL,
    user_provider uuid NOT NULL,
    price numeric(10,2) NOT NULL,
    end_date timestamp without time zone,
    created_at timestamp without time zone NOT NULL,
    CONSTRAINT offering_price_check CHECK ((price >= (0)::numeric))
);


ALTER TABLE public.offering OWNER TO postgres;

--
-- Name: poll; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.poll (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    title character varying(255) NOT NULL,
    description text,
    area_id uuid,
    building_id uuid,
    created_by uuid NOT NULL,
    start_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    end_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    anonymous boolean NOT NULL,
    finished boolean,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT area_or_building_not_both_null_or_not_null_polls CHECK ((((area_id IS NULL) AND (building_id IS NOT NULL)) OR ((area_id IS NOT NULL) AND (building_id IS NULL))))
);


ALTER TABLE public.poll OWNER TO postgres;

--
-- Name: poll_option; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.poll_option (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    poll_id uuid NOT NULL,
    option_text character varying(255) NOT NULL,
    option_votes integer DEFAULT 0 NOT NULL
);


ALTER TABLE public.poll_option OWNER TO postgres;

--
-- Name: poll_result; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.poll_result (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    poll_id uuid NOT NULL,
    voters_count integer,
    voting_ended boolean NOT NULL
);


ALTER TABLE public.poll_result OWNER TO postgres;

--
-- Name: poll_vote; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.poll_vote (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    poll_id uuid NOT NULL,
    option_id uuid NOT NULL,
    user_id uuid NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.poll_vote OWNER TO postgres;

--
-- Name: poll_winner; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.poll_winner (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    poll_id uuid NOT NULL,
    option_id uuid NOT NULL
);


ALTER TABLE public.poll_winner OWNER TO postgres;

--
-- Name: post; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.post (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(256) NOT NULL,
    area_id uuid,
    building_id uuid,
    created_by uuid NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    content text NOT NULL,
    image_reference character varying(2048),
    related_date timestamp without time zone,
    post_type character varying(50) NOT NULL,
    start_date_time timestamp without time zone,
    end_date_time timestamp without time zone,
    location_name character varying(256),
    online_url character varying(2048),
    max_attendees integer,
    CONSTRAINT area_or_building_not_both_null_or_not_null_ann CHECK ((((area_id IS NULL) AND (building_id IS NOT NULL)) OR ((area_id IS NOT NULL) AND (building_id IS NULL))))
);


ALTER TABLE public.post OWNER TO postgres;

--
-- Name: request; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.request (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    title character varying(50) NOT NULL,
    description text NOT NULL,
    is_active boolean DEFAULT true,
    area_id uuid NOT NULL,
    user_in_need uuid NOT NULL,
    status character varying(50) NOT NULL,
    created_at timestamp without time zone NOT NULL
);


ALTER TABLE public.request OWNER TO postgres;

--
-- Name: request_donor; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.request_donor (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    request_id uuid NOT NULL,
    donor_id uuid NOT NULL,
    approved_at timestamp without time zone NOT NULL,
    deadline_at timestamp without time zone
);


ALTER TABLE public.request_donor OWNER TO postgres;

--
-- Name: user_role; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_role (
    id uuid NOT NULL,
    user_role character varying(15) NOT NULL
);


ALTER TABLE public.user_role OWNER TO postgres;

--
-- Data for Name: app_user; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.app_user (id, first_name, last_name, email, username, password, user_role, status, created_at, building_id) FROM stdin;
19834194-1951-4cd2-a16a-c136f5ab6d5c	Dorian	Guz	dorian@gmail.com	Dorian	$2a$10$bNPJIlOJTw7gRktVdsgnou0ceHjn0uZVz2fodXjfEN7k7zdxjCo7O	b9e2a7f4-6c1d-47d2-8e93-45ab2c1d3f27	ACTIVE	2026-01-20 13:15:55.516004	\N
2a89daf3-cf68-4497-894d-ffa4a212804e	Natalia	Witosz	natalia@gmail.com	Natalia	$2a$10$tM7A3/iQHPooZdWuWyjyAuSgVS44pMKY3M.IQhmAmaCG8k/hl2NLi	a3f5c9d2-4b8e-4d61-9a67-12c4e9b7f8a1	ACTIVE	2026-01-20 13:18:18.994456	5084db84-0a56-492d-b538-8a5f1382aa24
b33a37ed-f3cf-4058-a592-0a00be9bae03	Paweł	Czanasz	pawel@gmail.com	Pawel	$2a$10$ewtjdGhW1/svGoxq5ziYvuNBahM53d02YC12g1WyBliJIBMimNvEO	a3f5c9d2-4b8e-4d61-9a67-12c4e9b7f8a1	ACTIVE	2026-01-20 13:50:25.733649	5084db84-0a56-492d-b538-8a5f1382aa24
35325533-9341-45b2-99bc-221499f5e08f	Jarek	Pater	jarek@gmail.com	Jarek	$2a$10$/ZhY0dqxPtixm3wGvosy3uMvofwMAgodDUA2zW3KQ0xDbsy5M/eRS	a3f5c9d2-4b8e-4d61-9a67-12c4e9b7f8a1	ACTIVE	2026-01-20 13:50:55.433047	5084db84-0a56-492d-b538-8a5f1382aa24
\.


--
-- Data for Name: area; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.area (id, name, country, city, type, created_at) FROM stdin;
04afff1c-1b55-4301-8491-fc2cb98561e9	Kind Area	Poland	Cracow	ESTATE	2026-01-20 13:17:14.412386
\.


--
-- Data for Name: building; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.building (id, name, country, city, street, number, area_id) FROM stdin;
5084db84-0a56-492d-b538-8a5f1382aa24	Building A	Poland	Cracow	Beer	5	04afff1c-1b55-4301-8491-fc2cb98561e9
\.


--
-- Data for Name: building_manager; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.building_manager (id, building_id, user_id) FROM stdin;
2f252742-4568-4568-9aa3-ad1481f2046d	5084db84-0a56-492d-b538-8a5f1382aa24	19834194-1951-4cd2-a16a-c136f5ab6d5c
\.


--
-- Data for Name: conversation; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.conversation (id, is_group, name, created_at, updated_at) FROM stdin;
7bb255d7-cc4d-4e32-98b0-a8fedcaf17db	f	\N	2026-01-20 13:18:57.051949	2026-01-20 13:18:57.051949
07c3bf29-2606-40f3-8c99-aa400f2b9b58	f	\N	2026-01-20 13:50:59.892382	2026-01-20 13:50:59.892382
dd757236-5cca-4f17-b5b1-0c729956b35d	f	\N	2026-01-20 13:50:59.892382	2026-01-20 13:50:59.892382
1c4a02a8-f25e-4d99-b19e-2ee1859944ac	f	\N	2026-01-20 13:51:01.94631	2026-01-20 13:51:01.94631
15965926-9c52-44e9-b8bd-49bbf9e3f61b	f	\N	2026-01-20 13:51:01.94631	2026-01-20 13:51:01.94631
574eb34c-bfcd-4e3c-bf55-978cd0e1db65	f	\N	2026-01-20 13:51:01.94631	2026-01-20 13:51:01.94631
9cc5923c-46eb-40f2-8b53-eec4ad2509ca	t	Group 1	2026-01-20 13:54:53.945104	2026-01-20 13:54:53.945104
\.


--
-- Data for Name: conversation_member; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.conversation_member (id, user_id, conversation_id, join_date) FROM stdin;
9234bdbb-c772-468b-9ed7-5ddc5502e06a	2a89daf3-cf68-4497-894d-ffa4a212804e	7bb255d7-cc4d-4e32-98b0-a8fedcaf17db	2026-01-20 13:18:57.051949
716fd475-3b9e-43c6-af11-e2bf163c70a5	19834194-1951-4cd2-a16a-c136f5ab6d5c	7bb255d7-cc4d-4e32-98b0-a8fedcaf17db	2026-01-20 13:18:57.051949
805d3723-af5f-48fc-a2bd-460ca0380e2d	b33a37ed-f3cf-4058-a592-0a00be9bae03	07c3bf29-2606-40f3-8c99-aa400f2b9b58	2026-01-20 13:50:59.892382
fc9e01ed-3db2-4c77-a7f7-cec4acf14f1f	2a89daf3-cf68-4497-894d-ffa4a212804e	07c3bf29-2606-40f3-8c99-aa400f2b9b58	2026-01-20 13:50:59.892382
720adfdf-4427-4c62-8a93-2b326e80ac87	b33a37ed-f3cf-4058-a592-0a00be9bae03	dd757236-5cca-4f17-b5b1-0c729956b35d	2026-01-20 13:50:59.892382
e8f438c4-2260-4154-a45a-4831f588d989	19834194-1951-4cd2-a16a-c136f5ab6d5c	dd757236-5cca-4f17-b5b1-0c729956b35d	2026-01-20 13:50:59.892382
f6794d45-c286-4cdd-80bc-95e612370ad0	35325533-9341-45b2-99bc-221499f5e08f	1c4a02a8-f25e-4d99-b19e-2ee1859944ac	2026-01-20 13:51:01.94631
11fc3534-3d56-436f-93bb-5afeb418f0f8	2a89daf3-cf68-4497-894d-ffa4a212804e	1c4a02a8-f25e-4d99-b19e-2ee1859944ac	2026-01-20 13:51:01.94631
b12c958c-c7fd-4a69-a652-8a925e0b018d	35325533-9341-45b2-99bc-221499f5e08f	15965926-9c52-44e9-b8bd-49bbf9e3f61b	2026-01-20 13:51:01.94631
d257dc60-870a-4fb5-aca9-b16642fe5db4	b33a37ed-f3cf-4058-a592-0a00be9bae03	15965926-9c52-44e9-b8bd-49bbf9e3f61b	2026-01-20 13:51:01.94631
9066122b-a44c-4f51-b0c6-d990e7974a56	35325533-9341-45b2-99bc-221499f5e08f	574eb34c-bfcd-4e3c-bf55-978cd0e1db65	2026-01-20 13:51:01.94631
3db91954-02c9-4ba3-b812-e3be2d793b6c	19834194-1951-4cd2-a16a-c136f5ab6d5c	574eb34c-bfcd-4e3c-bf55-978cd0e1db65	2026-01-20 13:51:01.94631
70fa411f-4573-4061-9a82-0455c7ae21e0	b33a37ed-f3cf-4058-a592-0a00be9bae03	9cc5923c-46eb-40f2-8b53-eec4ad2509ca	2026-01-20 13:54:53.954175
7fb08b7e-5600-4c1c-a0f2-27701536be0a	35325533-9341-45b2-99bc-221499f5e08f	9cc5923c-46eb-40f2-8b53-eec4ad2509ca	2026-01-20 13:54:53.954175
0ab276da-339d-4591-8bd8-70d9109382bd	2a89daf3-cf68-4497-894d-ffa4a212804e	9cc5923c-46eb-40f2-8b53-eec4ad2509ca	2026-01-20 13:54:53.959881
\.


--
-- Data for Name: facility; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.facility (id, name, building_id, type, capacity, location, requires_approval) FROM stdin;
774b1577-2f92-4b90-83cb-44eb4609643b	Conference Room	5084db84-0a56-492d-b538-8a5f1382aa24	RECREATION	100	Floor 1	f
\.


--
-- Data for Name: facility_reservation; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.facility_reservation (id, facility_id, user_id, start_time, end_time, status, purpose, created_at) FROM stdin;
940a2c33-3772-46cd-b4ea-8bd25cd963e9	774b1577-2f92-4b90-83cb-44eb4609643b	2a89daf3-cf68-4497-894d-ffa4a212804e	2026-01-22 00:30:00	2026-01-22 01:00:00	RESERVED	Presenation	2026-01-20 13:46:29.911056
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	0.0.0	init	SQL	V0_0_0__init.sql	827036935	postgres	2026-01-20 13:14:02.83241	119	t
2	0.0.1	facilities type	SQL	V0_0_1__facilities_type.sql	650963867	postgres	2026-01-20 13:14:02.985862	3	t
\.


--
-- Data for Name: issue; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.issue (id, title, description, status, priority, area_id, building_id, facility_id, poll_id, notify_everyone, created_by, created_at, issue_object) FROM stdin;
4ec47e43-392e-4e5e-a35e-ba45015ce1e7	Broken fountain	Phasellus ultricies nisi lorem, eu aliquet orci pharetra in. Donec a bibendum lorem. 	SUBMITTED	LOW	04afff1c-1b55-4301-8491-fc2cb98561e9	\N	\N	\N	f	2a89daf3-cf68-4497-894d-ffa4a212804e	2026-01-20 13:49:04.198088	AREA
ed55096d-ff95-4894-b562-9992ef38d8ce	Loud noise	Phasellus ultricies nisi lorem, eu aliquet orci pharetra in. Donec a bibendum lorem. Donec aliquam, est porttitor bibendum feugiat, nulla massa pellentesque lorem, vitae suscipit lacus orci sit amet ex. Interdum et malesuada fames ac ante ipsum primis in faucibus. Etiam posuere nunc ut sodales sagittis. 	SEEN_BY_RECIPIENT	HIGH	\N	5084db84-0a56-492d-b538-8a5f1382aa24	\N	\N	f	2a89daf3-cf68-4497-894d-ffa4a212804e	2026-01-20 13:48:36.40825	BUILDING
\.


--
-- Data for Name: message; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.message (id, sender_id, conversation_id, content, send_at) FROM stdin;
96e38176-93df-4bdb-a5b3-3e3612b157b2	19834194-1951-4cd2-a16a-c136f5ab6d5c	7bb255d7-cc4d-4e32-98b0-a8fedcaf17db	Hello, welcome in the kind area	2026-01-20 13:42:29.985905
9e46a516-b7a0-4b0e-9713-996acbc7b69e	2a89daf3-cf68-4497-894d-ffa4a212804e	7bb255d7-cc4d-4e32-98b0-a8fedcaf17db	Hi nice to meet you	2026-01-20 13:49:39.28692
\.


--
-- Data for Name: messages_read; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.messages_read (message_id, viewed_by, viewed_at) FROM stdin;
\.


--
-- Data for Name: notification; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.notification (id, issue_id, recipient_id, seen_at) FROM stdin;
a83048eb-97c2-4c8d-851e-ddec2d07971c	4ec47e43-392e-4e5e-a35e-ba45015ce1e7	19834194-1951-4cd2-a16a-c136f5ab6d5c	\N
3ffafc0b-f4f3-48ce-aea4-85fcd675e7b8	ed55096d-ff95-4894-b562-9992ef38d8ce	19834194-1951-4cd2-a16a-c136f5ab6d5c	2026-01-20 13:49:19.635919
\.


--
-- Data for Name: offering; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.offering (id, title, description, category, is_active, area_id, user_provider, price, end_date, created_at) FROM stdin;
25eac4fb-8247-4f6e-a2ef-3ff6fa483432	Math tutoring	Phasellus ultricies nisi lorem, eu aliquet orci pharetra in. Donec a bibendum lorem. Donec aliquam, est porttitor bibendum feugiat, nulla massa pellentesque lorem, vitae suscipit lacus orci sit amet ex. Interdum et malesuada fames ac ante ipsum primis in faucibus. Etiam posuere nunc ut sodales sagittis. 	TUTORING	t	04afff1c-1b55-4301-8491-fc2cb98561e9	2a89daf3-cf68-4497-894d-ffa4a212804e	50.00	\N	2026-01-20 13:47:17.891326
\.


--
-- Data for Name: poll; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.poll (id, title, description, area_id, building_id, created_by, start_time, end_time, anonymous, finished, created_at) FROM stdin;
aad6c2a3-71c9-4a0b-83e8-0a542b64a0b2	New wall color 	Phasellus ultricies nisi lorem, eu aliquet orci pharetra in. Donec a bibendum lorem. Donec aliquam, est porttitor bibendum feugiat, nulla massa pellentesque lorem, vitae suscipit lacus orci sit amet ex. Interdum et malesuada fames ac ante ipsum primis in faucibus. Etiam posuere nunc ut sodales sagittis. 	\N	5084db84-0a56-492d-b538-8a5f1382aa24	19834194-1951-4cd2-a16a-c136f5ab6d5c	2026-01-19 23:00:00	2026-01-20 12:00:00	f	t	2026-01-20 13:25:08.785206
\.


--
-- Data for Name: poll_option; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.poll_option (id, poll_id, option_text, option_votes) FROM stdin;
7a0f05d7-e0a8-4268-9d52-a5dab4f5ae15	aad6c2a3-71c9-4a0b-83e8-0a542b64a0b2	Yellow	1
c339f720-e986-4c97-9279-da4366ddbe76	aad6c2a3-71c9-4a0b-83e8-0a542b64a0b2	Light blue	2
\.


--
-- Data for Name: poll_result; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.poll_result (id, poll_id, voters_count, voting_ended) FROM stdin;
588292ad-b2a4-4aaf-bf07-9d88ce400c98	aad6c2a3-71c9-4a0b-83e8-0a542b64a0b2	\N	f
03d858c9-3b9c-4ee4-b411-a770c485b87f	aad6c2a3-71c9-4a0b-83e8-0a542b64a0b2	3	t
\.


--
-- Data for Name: poll_vote; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.poll_vote (id, poll_id, option_id, user_id, created_at) FROM stdin;
a2b1c4d5-102d-4007-afc7-9469d492271e	aad6c2a3-71c9-4a0b-83e8-0a542b64a0b2	c339f720-e986-4c97-9279-da4366ddbe76	2a89daf3-cf68-4497-894d-ffa4a212804e	2026-01-20 13:48:05.421003
4a636266-54c2-46d6-8cf2-dbb3f80214d6	aad6c2a3-71c9-4a0b-83e8-0a542b64a0b2	7a0f05d7-e0a8-4268-9d52-a5dab4f5ae15	35325533-9341-45b2-99bc-221499f5e08f	2026-01-20 13:51:43.061457
d7274c8e-48c5-4e54-a70c-a536aafc1680	aad6c2a3-71c9-4a0b-83e8-0a542b64a0b2	c339f720-e986-4c97-9279-da4366ddbe76	b33a37ed-f3cf-4058-a592-0a00be9bae03	2026-01-20 13:51:58.046046
\.


--
-- Data for Name: poll_winner; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.poll_winner (id, poll_id, option_id) FROM stdin;
3deae38a-5e08-477f-a94e-24475e858e8b	aad6c2a3-71c9-4a0b-83e8-0a542b64a0b2	c339f720-e986-4c97-9279-da4366ddbe76
\.


--
-- Data for Name: post; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.post (id, name, area_id, building_id, created_by, created_at, content, image_reference, related_date, post_type, start_date_time, end_date_time, location_name, online_url, max_attendees) FROM stdin;
91e2a7c0-a90a-4ed6-9d6c-f39251251a4b	Gas instalation check	\N	5084db84-0a56-492d-b538-8a5f1382aa24	19834194-1951-4cd2-a16a-c136f5ab6d5c	2026-01-20 13:20:18.679595	Lorem ipsum dolor sit amet, consectetur adipiscing elit. In varius at erat et pretium. Curabitur cursus turpis in convallis finibus. Interdum et malesuada fames ac ante ipsum primis in faucibus. Maecenas venenatis tellus mi, sed ornare massa tristique id. Donec consectetur neque sapien, sed blandit magna volutpat a. Cras mattis condimentum eros. Aenean mollis et orci a accumsan. In porttitor faucibus lorem, gravida vestibulum massa ultrices ac. Donec posuere dui a sem feugiat aliquam. Maecenas porttitor malesuada orci, vel elementum felis iaculis a. Suspendisse ac nulla nisl. 	\N	2026-01-29 23:00:00	ANNOUNCEMENT	\N	\N	\N	\N	\N
ecdaf17e-f9a1-48c3-bad4-57dea8b481d0	Stairs renovation	\N	5084db84-0a56-492d-b538-8a5f1382aa24	19834194-1951-4cd2-a16a-c136f5ab6d5c	2026-01-20 13:21:27.66081	Lorem ipsum dolor sit amet, consectetur adipiscing elit. In varius at erat et pretium. Curabitur cursus turpis in convallis finibus. Interdum et malesuada fames ac ante ipsum primis in faucibus. Maecenas venenatis tellus mi, sed ornare massa tristique id. Donec consectetur neque sapien, sed blandit magna volutpat a. Cras mattis condimentum eros. Aenean mollis et orci a accumsan. In porttitor faucibus lorem, gravida vestibulum massa ultrices ac. Donec posuere dui a sem feugiat aliquam. Maecenas porttitor malesuada orci, vel elementum felis iaculis a. Suspendisse ac nulla nisl. Lorem ipsum dolor sit amet, consectetur adipiscing elit. In varius at erat et pretium. Curabitur cursus turpis in convallis finibus. Interdum et malesuada fames ac ante ipsum primis in faucibus. Maecenas venenatis tellus mi, sed ornare massa tristique id. Donec consectetur neque sapien, sed blandit magna volutpat a. Cras mattis condimentum eros. Aenean mollis et orci a accumsan. In porttitor faucibus lorem, gravida vestibulum massa ultrices ac. Donec posuere dui a sem feugiat aliquam. Maecenas porttitor malesuada orci, vel elementum felis iaculis a. Suspendisse ac nulla nisl. 	\N	2026-02-10 23:00:00	ANNOUNCEMENT	\N	\N	\N	\N	\N
99482775-bd69-4a14-ac46-907ce1f5831a	Jogging	\N	5084db84-0a56-492d-b538-8a5f1382aa24	2a89daf3-cf68-4497-894d-ffa4a212804e	2026-01-20 13:45:43.111255	Phasellus ultricies nisi lorem, eu aliquet orci pharetra in. Donec a bibendum lorem. Donec aliquam, est porttitor bibendum feugiat, nulla massa pellentesque lorem, vitae suscipit lacus orci sit amet ex. Interdum et malesuada fames ac ante ipsum primis in faucibus. Etiam posuere nunc ut sodales sagittis. 	\N	2026-01-27 23:00:00	EVENT	2026-01-27 23:00:00	2026-01-28 23:00:00	Garden	https://theuselessweb.com/	\N
\.


--
-- Data for Name: request; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.request (id, title, description, is_active, area_id, user_in_need, status, created_at) FROM stdin;
\.


--
-- Data for Name: request_donor; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.request_donor (id, request_id, donor_id, approved_at, deadline_at) FROM stdin;
\.


--
-- Data for Name: user_role; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_role (id, user_role) FROM stdin;
a3f5c9d2-4b8e-4d61-9a67-12c4e9b7f8a1	RESIDENT
b9e2a7f4-6c1d-47d2-8e93-45ab2c1d3f27	MANAGER
c4d8e1a9-9f2b-4c6f-82d5-67d8a1c2e5b3	ADMIN
\.


--
-- Name: app_user app_user_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT app_user_email_key UNIQUE (email);


--
-- Name: app_user app_user_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT app_user_pkey PRIMARY KEY (id);


--
-- Name: app_user app_user_username_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT app_user_username_key UNIQUE (username);


--
-- Name: area area_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.area
    ADD CONSTRAINT area_pkey PRIMARY KEY (id);


--
-- Name: building_manager building_manager_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.building_manager
    ADD CONSTRAINT building_manager_pkey PRIMARY KEY (id);


--
-- Name: building building_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.building
    ADD CONSTRAINT building_pkey PRIMARY KEY (id);


--
-- Name: conversation_member conversation_member_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.conversation_member
    ADD CONSTRAINT conversation_member_pkey PRIMARY KEY (id);


--
-- Name: conversation conversation_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.conversation
    ADD CONSTRAINT conversation_pkey PRIMARY KEY (id);


--
-- Name: facility facility_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.facility
    ADD CONSTRAINT facility_pkey PRIMARY KEY (id);


--
-- Name: facility_reservation facility_reservation_facility_id_start_time_end_time_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.facility_reservation
    ADD CONSTRAINT facility_reservation_facility_id_start_time_end_time_key UNIQUE (facility_id, start_time, end_time);


--
-- Name: facility_reservation facility_reservation_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.facility_reservation
    ADD CONSTRAINT facility_reservation_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: issue issue_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.issue
    ADD CONSTRAINT issue_pkey PRIMARY KEY (id);


--
-- Name: message message_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message
    ADD CONSTRAINT message_pkey PRIMARY KEY (id);


--
-- Name: messages_read messages_read_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.messages_read
    ADD CONSTRAINT messages_read_pkey PRIMARY KEY (message_id, viewed_by);


--
-- Name: notification notification_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notification
    ADD CONSTRAINT notification_pkey PRIMARY KEY (id);


--
-- Name: offering offering_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.offering
    ADD CONSTRAINT offering_pkey PRIMARY KEY (id);


--
-- Name: poll_option poll_option_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.poll_option
    ADD CONSTRAINT poll_option_pkey PRIMARY KEY (id);


--
-- Name: poll_option poll_option_poll_id_option_text_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.poll_option
    ADD CONSTRAINT poll_option_poll_id_option_text_key UNIQUE (poll_id, option_text);


--
-- Name: poll poll_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.poll
    ADD CONSTRAINT poll_pkey PRIMARY KEY (id);


--
-- Name: poll_result poll_result_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.poll_result
    ADD CONSTRAINT poll_result_pkey PRIMARY KEY (id);


--
-- Name: poll_vote poll_vote_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.poll_vote
    ADD CONSTRAINT poll_vote_pkey PRIMARY KEY (id);


--
-- Name: poll_vote poll_vote_poll_id_user_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.poll_vote
    ADD CONSTRAINT poll_vote_poll_id_user_id_key UNIQUE (poll_id, user_id);


--
-- Name: poll_winner poll_winner_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.poll_winner
    ADD CONSTRAINT poll_winner_pkey PRIMARY KEY (id);


--
-- Name: post post_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.post
    ADD CONSTRAINT post_pkey PRIMARY KEY (id);


--
-- Name: request_donor request_donor_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.request_donor
    ADD CONSTRAINT request_donor_pkey PRIMARY KEY (id);


--
-- Name: request request_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.request
    ADD CONSTRAINT request_pkey PRIMARY KEY (id);


--
-- Name: user_role user_role_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_role
    ADD CONSTRAINT user_role_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: app_user trg_check_user_building_id; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_check_user_building_id BEFORE INSERT OR UPDATE ON public.app_user FOR EACH ROW EXECUTE FUNCTION public.enforce_user_building_id_rule();


--
-- Name: app_user app_user_building_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT app_user_building_id_fkey FOREIGN KEY (building_id) REFERENCES public.building(id) ON DELETE CASCADE;


--
-- Name: app_user app_user_user_role_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT app_user_user_role_fkey FOREIGN KEY (user_role) REFERENCES public.user_role(id) ON DELETE CASCADE;


--
-- Name: building building_area_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.building
    ADD CONSTRAINT building_area_id_fkey FOREIGN KEY (area_id) REFERENCES public.area(id) ON DELETE CASCADE;


--
-- Name: building_manager building_manager_building_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.building_manager
    ADD CONSTRAINT building_manager_building_id_fkey FOREIGN KEY (building_id) REFERENCES public.building(id) ON DELETE CASCADE;


--
-- Name: building_manager building_manager_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.building_manager
    ADD CONSTRAINT building_manager_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.app_user(id) ON DELETE CASCADE;


--
-- Name: conversation_member conversation_member_conversation_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.conversation_member
    ADD CONSTRAINT conversation_member_conversation_id_fkey FOREIGN KEY (conversation_id) REFERENCES public.conversation(id) ON DELETE CASCADE;


--
-- Name: conversation_member conversation_member_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.conversation_member
    ADD CONSTRAINT conversation_member_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.app_user(id) ON DELETE CASCADE;


--
-- Name: facility facility_building_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.facility
    ADD CONSTRAINT facility_building_id_fkey FOREIGN KEY (building_id) REFERENCES public.building(id) ON DELETE CASCADE;


--
-- Name: facility_reservation facility_reservation_facility_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.facility_reservation
    ADD CONSTRAINT facility_reservation_facility_id_fkey FOREIGN KEY (facility_id) REFERENCES public.facility(id) ON DELETE CASCADE;


--
-- Name: facility_reservation facility_reservation_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.facility_reservation
    ADD CONSTRAINT facility_reservation_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.app_user(id) ON DELETE CASCADE;


--
-- Name: issue issue_area_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.issue
    ADD CONSTRAINT issue_area_id_fkey FOREIGN KEY (area_id) REFERENCES public.area(id) ON DELETE CASCADE;


--
-- Name: issue issue_building_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.issue
    ADD CONSTRAINT issue_building_id_fkey FOREIGN KEY (building_id) REFERENCES public.building(id) ON DELETE CASCADE;


--
-- Name: issue issue_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.issue
    ADD CONSTRAINT issue_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.app_user(id) ON DELETE CASCADE;


--
-- Name: issue issue_facility_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.issue
    ADD CONSTRAINT issue_facility_id_fkey FOREIGN KEY (facility_id) REFERENCES public.facility(id) ON DELETE CASCADE;


--
-- Name: issue issue_poll_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.issue
    ADD CONSTRAINT issue_poll_id_fkey FOREIGN KEY (poll_id) REFERENCES public.poll(id) ON DELETE CASCADE;


--
-- Name: message message_conversation_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message
    ADD CONSTRAINT message_conversation_id_fkey FOREIGN KEY (conversation_id) REFERENCES public.conversation(id) ON DELETE CASCADE;


--
-- Name: message message_sender_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message
    ADD CONSTRAINT message_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES public.app_user(id) ON DELETE CASCADE;


--
-- Name: messages_read messages_read_message_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.messages_read
    ADD CONSTRAINT messages_read_message_id_fkey FOREIGN KEY (message_id) REFERENCES public.message(id) ON DELETE CASCADE;


--
-- Name: messages_read messages_read_viewed_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.messages_read
    ADD CONSTRAINT messages_read_viewed_by_fkey FOREIGN KEY (viewed_by) REFERENCES public.app_user(id) ON DELETE CASCADE;


--
-- Name: notification notification_issue_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notification
    ADD CONSTRAINT notification_issue_id_fkey FOREIGN KEY (issue_id) REFERENCES public.issue(id) ON DELETE CASCADE;


--
-- Name: notification notification_recipient_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notification
    ADD CONSTRAINT notification_recipient_id_fkey FOREIGN KEY (recipient_id) REFERENCES public.app_user(id) ON DELETE CASCADE;


--
-- Name: offering offering_area_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.offering
    ADD CONSTRAINT offering_area_id_fkey FOREIGN KEY (area_id) REFERENCES public.area(id) ON DELETE CASCADE;


--
-- Name: offering offering_user_provider_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.offering
    ADD CONSTRAINT offering_user_provider_fkey FOREIGN KEY (user_provider) REFERENCES public.app_user(id) ON DELETE CASCADE;


--
-- Name: poll poll_area_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.poll
    ADD CONSTRAINT poll_area_id_fkey FOREIGN KEY (area_id) REFERENCES public.area(id) ON DELETE CASCADE;


--
-- Name: poll poll_building_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.poll
    ADD CONSTRAINT poll_building_id_fkey FOREIGN KEY (building_id) REFERENCES public.building(id) ON DELETE CASCADE;


--
-- Name: poll poll_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.poll
    ADD CONSTRAINT poll_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.app_user(id) ON DELETE CASCADE;


--
-- Name: poll_option poll_option_poll_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.poll_option
    ADD CONSTRAINT poll_option_poll_id_fkey FOREIGN KEY (poll_id) REFERENCES public.poll(id) ON DELETE CASCADE;


--
-- Name: poll_result poll_result_poll_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.poll_result
    ADD CONSTRAINT poll_result_poll_id_fkey FOREIGN KEY (poll_id) REFERENCES public.poll(id) ON DELETE CASCADE;


--
-- Name: poll_vote poll_vote_option_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.poll_vote
    ADD CONSTRAINT poll_vote_option_id_fkey FOREIGN KEY (option_id) REFERENCES public.poll_option(id) ON DELETE CASCADE;


--
-- Name: poll_vote poll_vote_poll_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.poll_vote
    ADD CONSTRAINT poll_vote_poll_id_fkey FOREIGN KEY (poll_id) REFERENCES public.poll(id) ON DELETE CASCADE;


--
-- Name: poll_vote poll_vote_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.poll_vote
    ADD CONSTRAINT poll_vote_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.app_user(id) ON DELETE CASCADE;


--
-- Name: poll_winner poll_winner_option_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.poll_winner
    ADD CONSTRAINT poll_winner_option_id_fkey FOREIGN KEY (option_id) REFERENCES public.poll_option(id) ON DELETE CASCADE;


--
-- Name: poll_winner poll_winner_poll_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.poll_winner
    ADD CONSTRAINT poll_winner_poll_id_fkey FOREIGN KEY (poll_id) REFERENCES public.poll(id) ON DELETE CASCADE;


--
-- Name: post post_area_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.post
    ADD CONSTRAINT post_area_id_fkey FOREIGN KEY (area_id) REFERENCES public.area(id) ON DELETE CASCADE;


--
-- Name: post post_building_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.post
    ADD CONSTRAINT post_building_id_fkey FOREIGN KEY (building_id) REFERENCES public.building(id) ON DELETE CASCADE;


--
-- Name: post post_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.post
    ADD CONSTRAINT post_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.app_user(id) ON DELETE CASCADE;


--
-- Name: request request_area_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.request
    ADD CONSTRAINT request_area_id_fkey FOREIGN KEY (area_id) REFERENCES public.area(id) ON DELETE CASCADE;


--
-- Name: request_donor request_donor_donor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.request_donor
    ADD CONSTRAINT request_donor_donor_id_fkey FOREIGN KEY (donor_id) REFERENCES public.app_user(id) ON DELETE CASCADE;


--
-- Name: request_donor request_donor_request_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.request_donor
    ADD CONSTRAINT request_donor_request_id_fkey FOREIGN KEY (request_id) REFERENCES public.request(id) ON DELETE CASCADE;


--
-- Name: request request_user_in_need_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.request
    ADD CONSTRAINT request_user_in_need_fkey FOREIGN KEY (user_in_need) REFERENCES public.app_user(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict bqpdEOUscPBTiiV0rF1iSPnYygLPxWWP8HIbfZRVPDbY2pYBkbtKXU5QkYUhuPw

