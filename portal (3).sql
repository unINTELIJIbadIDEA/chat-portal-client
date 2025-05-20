git -- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Maj 20, 2025 at 10:55 PM
-- Wersja serwera: 10.4.32-MariaDB
-- Wersja PHP: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `portal`
--

-- --------------------------------------------------------

--
-- Struktura tabeli dla tabeli `conversations`
--

CREATE TABLE `conversations` (
  `roomId` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `conversations`
--

INSERT INTO `conversations` (`roomId`, `password`) VALUES
('a', 'a'),
('chat-123', 'secret123'),
('cosik', 'cosik'),
('dawid', 'brach'),
('kacper', 'kacper'),
('krzysztof', 'koszyk'),
('marcin', 'marcin'),
('room123', 'secret'),
('room124', 'secret'),
('tak o', '1');

-- --------------------------------------------------------

--
-- Struktura tabeli dla tabeli `messages`
--

CREATE TABLE `messages` (
  `message_id` int(11) NOT NULL,
  `chat_id` varchar(255) NOT NULL,
  `sender_id` int(11) NOT NULL,
  `content` text NOT NULL,
  `time` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `messages`
--

INSERT INTO `messages` (`message_id`, `chat_id`, `sender_id`, `content`, `time`) VALUES
(1, 'chat2', 5, 'cosik', '2025-05-14 18:50:13'),
(2, 'chat2', 5, 'cosik innego', '2025-05-14 18:50:23'),
(3, 'chat2', 5, 'xd', '2025-05-14 18:50:30'),
(4, 'marcin', 6, 'adsadsads', '2025-05-14 20:06:02'),
(5, 'cosik', 6, 'dsadsdasads', '2025-05-14 20:06:06'),
(6, 'marcin', 5, 'xd', '2025-05-14 20:06:12'),
(7, 'kacper', 6, 'sdfsfdsdfsfdsfdsdf', '2025-05-16 21:27:57'),
(8, 'kacper', 7, 'COSIKKASDKASDNASDJJADSHJADSHJASD', '2025-05-16 21:29:51'),
(9, 'krzysztof', 6, 'COSIK', '2025-05-20 15:02:41'),
(10, 'marcin', 6, 'ssdasdasdasdasdasdasd', '2025-05-20 15:02:49'),
(11, 'marcin', 6, 'cxdsafasdads', '2025-05-20 15:02:55'),
(12, 'tak o', 8, 'hejka ct', '2025-05-20 22:37:55'),
(13, 'tak o', 8, 'siema', '2025-05-20 22:52:49');

-- --------------------------------------------------------

--
-- Struktura tabeli dla tabeli `posts`
--

CREATE TABLE `posts` (
  `postId` int(11) NOT NULL,
  `userId` int(11) NOT NULL,
  `content` text NOT NULL,
  `date` date NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `posts`
--

INSERT INTO `posts` (`postId`, `userId`, `content`, `date`) VALUES
(3, 4, 'Słaby raper pozdro', '2025-03-29'),
(4, 8, 'sdfg', '2025-05-20'),
(6, 8, 'asdasdsda', '2025-05-20');

-- --------------------------------------------------------

--
-- Struktura tabeli dla tabeli `questions`
--

CREATE TABLE `questions` (
  `id` int(11) NOT NULL,
  `text` text NOT NULL,
  `option_a` text NOT NULL,
  `option_b` text NOT NULL,
  `option_c` text NOT NULL,
  `option_d` text NOT NULL,
  `correct_option` char(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Struktura tabeli dla tabeli `user`
--

CREATE TABLE `user` (
  `userId` int(11) NOT NULL,
  `name` varchar(70) NOT NULL,
  `surname` varchar(70) NOT NULL,
  `nickname` varchar(100) NOT NULL,
  `email` varchar(150) NOT NULL,
  `birthday` date NOT NULL DEFAULT current_timestamp(),
  `password` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `user`
--

INSERT INTO `user` (`userId`, `name`, `surname`, `nickname`, `email`, `birthday`, `password`) VALUES
(4, 'Jan', 'Rapowanie', 'Janek123', 'janek@gmail.com', '2025-03-29', 'jr123!'),
(5, 'Marcin', 'Zawiski', 'm.zawiski03@gmail.com', 'm.zawiski03@gmail.com', '2003-10-10', 'zaq1@WSX'),
(6, 'cosik', 'cosik', 'cosik', 'cosik', '2003-10-10', 'cosik'),
(7, 'kacper', 'tomera', 'kacperek', 'kacperek', '2025-05-14', 'kacperek'),
(8, 'a', 'a', 'a', 'a', '2025-05-20', 'a');

-- --------------------------------------------------------

--
-- Struktura tabeli dla tabeli `usersconversations`
--

CREATE TABLE `usersconversations` (
  `userId` int(11) NOT NULL,
  `conversationId` varchar(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `usersconversations`
--

INSERT INTO `usersconversations` (`userId`, `conversationId`) VALUES
(5, 'chat-123'),
(5, 'cosik'),
(5, 'marcin'),
(5, 'room123'),
(5, 'room124'),
(6, 'cosik'),
(6, 'dawid'),
(6, 'kacper'),
(6, 'krzysztof'),
(6, 'marcin'),
(7, 'kacper'),
(8, 'a'),
(8, 'tak o');

--
-- Indeksy dla zrzutów tabel
--

--
-- Indeksy dla tabeli `conversations`
--
ALTER TABLE `conversations`
  ADD PRIMARY KEY (`roomId`);

--
-- Indeksy dla tabeli `messages`
--
ALTER TABLE `messages`
  ADD PRIMARY KEY (`message_id`);

--
-- Indeksy dla tabeli `posts`
--
ALTER TABLE `posts`
  ADD PRIMARY KEY (`postId`),
  ADD KEY `posts` (`userId`);

--
-- Indeksy dla tabeli `questions`
--
ALTER TABLE `questions`
  ADD PRIMARY KEY (`id`);

--
-- Indeksy dla tabeli `user`
--
ALTER TABLE `user`
  ADD PRIMARY KEY (`userId`);

--
-- Indeksy dla tabeli `usersconversations`
--
ALTER TABLE `usersconversations`
  ADD PRIMARY KEY (`userId`,`conversationId`),
  ADD KEY `conversationId` (`conversationId`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `messages`
--
ALTER TABLE `messages`
  MODIFY `message_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=14;

--
-- AUTO_INCREMENT for table `posts`
--
ALTER TABLE `posts`
  MODIFY `postId` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT for table `user`
--
ALTER TABLE `user`
  MODIFY `userId` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `posts`
--
ALTER TABLE `posts`
  ADD CONSTRAINT `posts` FOREIGN KEY (`userId`) REFERENCES `user` (`userId`);

--
-- Constraints for table `usersconversations`
--
ALTER TABLE `usersconversations`
  ADD CONSTRAINT `usersconversations_ibfk_1` FOREIGN KEY (`userId`) REFERENCES `user` (`userId`),
  ADD CONSTRAINT `usersconversations_ibfk_2` FOREIGN KEY (`conversationId`) REFERENCES `conversations` (`roomId`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
