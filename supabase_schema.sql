-- ====================================================================
-- KaziDaily Supabase Database Schema & Migration Script
-- Run this script in your Supabase SQL Editor to update your database.
-- ====================================================================

-- 1. Enable UUID Extension
create extension if not exists "uuid-ossp";

-- 2. Profiles Table (Linked to Supabase Auth users)
create table if not exists public.profiles (
    id uuid references auth.users on delete cascade primary key,
    full_name text not null,
    phone text,
    role_title text default 'Verified Artisan & Homeowner',
    bio text,
    skills text[] default array[]::text[],
    experience_years int default 0,
    average_rating numeric(3,2) default 5.00,
    total_reviews int default 0,
    jobs_completed_as_worker int default 0,
    jobs_posted_as_employer int default 0,
    is_phone_verified boolean default false,
    is_id_verified boolean default false,
    location text default 'Nairobi, Kenya',
    avatar_url text,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null,
    updated_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- 3. Jobs Table (Gig marketplace listings and escrow tracking)
create table if not exists public.jobs (
    id uuid default uuid_generate_v4() primary key,
    title text not null,
    description text not null,
    category text not null,
    pay_ksh int not null,
    duration_hours int not null,
    location_name text not null,
    latitude double precision not null,
    longitude double precision not null,
    employer_id uuid references public.profiles(id) on delete cascade not null,
    employer_name text not null,
    employer_rating numeric(3,2) default 4.9,
    employer_review_count int default 0,
    status text default 'OPEN' not null, -- OPEN, APPLIED, IN_PROGRESS, COMPLETED, DISPUTED
    escrow_amount_ksh int default 0,
    platform_fee_percent int default 10,
    assigned_worker_id uuid references public.profiles(id) on delete set null,
    assigned_worker_name text,
    worker_reviewed boolean default false,
    employer_reviewed boolean default false,
    photo_urls text[] default array[]::text[],
    created_at timestamp with time zone default timezone('utc'::text, now()) not null,
    updated_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- 4. Wallet Transactions Table
create table if not exists public.wallet_transactions (
    id uuid default uuid_generate_v4() primary key,
    user_id uuid references public.profiles(id) on delete cascade not null,
    title text not null,
    subtitle text,
    amount_ksh int not null,
    type text not null, -- ESCROW_DEPOSIT, ESCROW_RELEASE, WITHDRAWAL_MPESA, WITHDRAWAL_BANK
    is_positive boolean default true,
    reference_job_id uuid references public.jobs(id) on delete set null,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- 5. Reviews Table
create table if not exists public.reviews (
    id uuid default uuid_generate_v4() primary key,
    job_id uuid references public.jobs(id) on delete cascade not null,
    job_title text not null,
    reviewer_id uuid references public.profiles(id) on delete cascade not null,
    reviewer_name text not null,
    reviewer_role text not null,
    reviewee_id uuid references public.profiles(id) on delete cascade not null,
    reviewee_name text not null,
    rating int not null check (rating >= 1 and rating <= 5),
    tags text[] default array[]::text[],
    comment text,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- 6. Chat Threads & Messages Tables
create table if not exists public.chat_threads (
    thread_id uuid default uuid_generate_v4() primary key,
    job_id uuid references public.jobs(id) on delete cascade not null,
    job_title text not null,
    employer_id uuid references public.profiles(id) on delete cascade not null,
    worker_id uuid references public.profiles(id) on delete cascade not null,
    last_message text,
    last_timestamp timestamp with time zone default timezone('utc'::text, now()) not null,
    unread_count int default 0,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

create table if not exists public.chat_messages (
    id uuid default uuid_generate_v4() primary key,
    thread_id uuid references public.chat_threads(thread_id) on delete cascade not null,
    sender_id uuid references public.profiles(id) on delete cascade not null,
    sender_name text not null,
    text text not null,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- 7. Enable Row Level Security (RLS) on all tables
alter table public.profiles enable row level security;
alter table public.jobs enable row level security;
alter table public.wallet_transactions enable row level security;
alter table public.reviews enable row level security;
alter table public.chat_threads enable row level security;
alter table public.chat_messages enable row level security;

-- 8. Create RLS Policies for secure access
create policy "Profiles are viewable by everyone" on public.profiles for select using (true);
create policy "Users can update own profile" on public.profiles for update using (auth.uid() = id);

create policy "Jobs are viewable by everyone" on public.jobs for select using (true);
create policy "Authenticated users can create jobs" on public.jobs for insert with check (auth.uid() = employer_id);
create policy "Job employers and assigned workers can update jobs" on public.jobs for update using (auth.uid() = employer_id or auth.uid() = assigned_worker_id);

create policy "Users can view own wallet transactions" on public.wallet_transactions for select using (auth.uid() = user_id);
create policy "Users can insert own wallet transactions" on public.wallet_transactions for insert with check (auth.uid() = user_id);

create policy "Reviews are viewable by everyone" on public.reviews for select using (true);
create policy "Authenticated users can submit reviews" on public.reviews for insert with check (auth.uid() = reviewer_id);

create policy "Users can view their chat threads" on public.chat_threads for select using (auth.uid() = employer_id or auth.uid() = worker_id);
create policy "Users can create chat threads" on public.chat_threads for insert with check (auth.uid() = employer_id or auth.uid() = worker_id);

create policy "Users can view messages in their threads" on public.chat_messages for select using (
    exists (
        select 1 from public.chat_threads
        where chat_threads.thread_id = chat_messages.thread_id
        and (chat_threads.employer_id = auth.uid() or chat_threads.worker_id = auth.uid())
    )
);
create policy "Users can insert messages in their threads" on public.chat_messages for insert with check (
    auth.uid() = sender_id and
    exists (
        select 1 from public.chat_threads
        where chat_threads.thread_id = chat_messages.thread_id
        and (chat_threads.employer_id = auth.uid() or chat_threads.worker_id = auth.uid())
    )
);

-- 9. Enable Realtime on tables for live sync
alter publication supabase_realtime add table public.jobs;
alter publication supabase_realtime add table public.chat_messages;
alter publication supabase_realtime add table public.wallet_transactions;
